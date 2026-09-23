package com.luppo.farmacia.scraper.scheduler;

import com.luppo.farmacia.scraper.entity.*;
import com.luppo.farmacia.scraper.model.ScrapedItem;
import com.luppo.farmacia.scraper.repository.*;
import com.luppo.farmacia.scraper.service.ai.ContentHashService;
import com.luppo.farmacia.scraper.service.scraper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScrapingScheduler {

    @Value("${scraping.enabled:true}")
    private boolean scrapingEnabled;

    private final CefafaScraper cefafaScraper;
    private final EconomicasScraper economicasScraper;
    private final SanNicolasScraper sanNicolasScraper;
    private final CamilaScraper camilaScraper;
    private final SrsDataProvider srsDataProvider;

    private final PharmacyRepository pharmacyRepository;
    private final PharmacyProductRepository pharmacyProductRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ScrapingExecutionRepository scrapingExecutionRepository;
    private final ContentHashService contentHashService;

    // Ejecuta de 8am a 5pm en America/El_Salvador
    @Scheduled(cron = "${scraping.cron:0 0 8-17 * * *}", zone = "${scraping.timezone:America/El_Salvador}")
    public void runScheduledScraping() {
        if (!scrapingEnabled) {
            log.info("Scraping programado está desactivado por configuración.");
            return;
        }

        log.info("=== INICIANDO CICLO DE SCRAPING DE MEDICAMENTOS (EL SALVADOR) ===");
        runCefafaJob();
        runEconomicasJob();
        runSanNicolasJob();
        runCamilaJob();
        srsDataProvider.syncSrsReferences();
        log.info("=== CICLO DE SCRAPING COMPLETADO ===");
    }

    public void runCefafaJob() {
        executeJob(cefafaScraper);
    }

    public void runEconomicasJob() {
        executeJob(economicasScraper);
    }

    public void runSanNicolasJob() {
        executeJob(sanNicolasScraper);
    }

    public void runCamilaJob() {
        executeJob(camilaScraper);
    }

    private void executeJob(PharmacyScraper scraper) {
        log.info("Ejecutando Job para {}", scraper.getPharmacyName());
        Pharmacy pharmacy = pharmacyRepository.findByCode(scraper.getPharmacyCode()).orElse(null);

        ScrapingExecution execution = ScrapingExecution.builder()
                .pharmacy(pharmacy)
                .status("RUNNING")
                .startedAt(OffsetDateTime.now())
                .recordsFound(0)
                .recordsCreated(0)
                .recordsUpdated(0)
                .recordsFailed(0)
                .build();
        execution = scrapingExecutionRepository.save(execution);

        try {
            // Buscamos productos clave de alta rotación
            String[] targetQueries = {"acetaminofen", "ibuprofeno", "loratadina", "amoxicilina"};
            int created = 0;
            int updated = 0;
            int totalFound = 0;

            for (String q : targetQueries) {
                List<ScrapedItem> items = scraper.search(q);
                totalFound += items.size();

                if (pharmacy != null) {
                    for (ScrapedItem item : items) {
                        try {
                            String newHash = contentHashService.computeHash(
                                    item.getOriginalName(),
                                    item.getPrice() != null ? item.getPrice().toString() : "",
                                    item.getOfferPrice() != null ? item.getOfferPrice().toString() : ""
                            );

                            PharmacyProduct product = pharmacyProductRepository
                                    .findByPharmacyIdAndExternalId(pharmacy.getId(), item.getExternalId())
                                    .orElse(null);

                            if (product == null) {
                                product = PharmacyProduct.builder()
                                        .pharmacy(pharmacy)
                                        .externalId(item.getExternalId())
                                        .originalName(item.getOriginalName())
                                        .brand(item.getBrand())
                                        .presentation(item.getPresentation())
                                        .url(item.getUrl())
                                        .imageUrl(item.getImageUrl())
                                        .currentPrice(item.getPrice())
                                        .currentOfferPrice(item.getOfferPrice())
                                        .isAvailable(item.getIsAvailable())
                                        .contentHash(newHash)
                                        .lastScrapedAt(OffsetDateTime.now())
                                        .build();
                                pharmacyProductRepository.save(product);
                                created++;
                            } else {
                                product.setCurrentPrice(item.getPrice());
                                product.setCurrentOfferPrice(item.getOfferPrice());
                                product.setIsAvailable(item.getIsAvailable());
                                product.setContentHash(newHash);
                                product.setLastScrapedAt(OffsetDateTime.now());
                                pharmacyProductRepository.save(product);
                                updated++;
                            }

                            // Registro inmutable de precio
                            if (item.getPrice() != null) {
                                priceHistoryRepository.save(PriceHistory.builder()
                                        .pharmacyProduct(product)
                                        .price(item.getPrice())
                                        .offerPrice(item.getOfferPrice())
                                        .isAvailable(item.getIsAvailable())
                                        .checkedAt(OffsetDateTime.now())
                                        .build());
                            }
                        } catch (Exception ex) {
                            log.warn("Error guardando producto de {}: {}", scraper.getPharmacyCode(), ex.getMessage());
                        }
                    }
                }
            }

            execution.setStatus("COMPLETED");
            execution.setFinishedAt(OffsetDateTime.now());
            execution.setRecordsFound(totalFound);
            execution.setRecordsCreated(created);
            execution.setRecordsUpdated(updated);
            scrapingExecutionRepository.save(execution);
            log.info("Job {} completado. Encontrados: {}, Creados: {}, Actualizados: {}", 
                    scraper.getPharmacyCode(), totalFound, created, updated);

        } catch (Exception e) {
            log.error("Fallo en job {}: {}", scraper.getPharmacyCode(), e.getMessage());
            execution.setStatus("FAILED");
            execution.setFinishedAt(OffsetDateTime.now());
            execution.setErrorMessage(e.getMessage());
            scrapingExecutionRepository.save(execution);
        }
    }
}
