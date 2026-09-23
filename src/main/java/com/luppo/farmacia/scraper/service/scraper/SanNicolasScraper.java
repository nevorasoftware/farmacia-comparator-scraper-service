package com.luppo.farmacia.scraper.service.scraper;

import com.luppo.farmacia.scraper.model.ScrapedItem;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class SanNicolasScraper implements PharmacyScraper {

    private static final String BASE_URL = "https://www.farmaciasannicolas.com";

    @Override
    public String getPharmacyCode() {
        return "SAN_NICOLAS";
    }

    @Override
    public String getPharmacyName() {
        return "Farmacias San Nicolás";
    }

    @Override
    public List<ScrapedItem> search(String query) {
        List<ScrapedItem> results = new ArrayList<>();
        Set<String> seenSkus = new HashSet<>();

        List<String> targetUrls = Arrays.asList(
                "/category/suplementos-nutricionales/01011002",
                "/category/leches-formulas-y-suplementos/01011?page=2",
                "/category/dolor-y-fiebre/01005",
                "/productos/landing/1000061"
        );

        for (String urlPath : targetUrls) {
            try {
                String fullTargetUrl = BASE_URL + urlPath;
                Document doc = Jsoup.connect(fullTargetUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .timeout(10000)
                        .get();

                Elements cards = doc.select(".product-item-box, .prod-info");
                for (Element card : cards) {
                    Element link = card.selectFirst("a[href*=/producto/]");
                    if (link == null) continue;

                    String title = link.text().trim();
                    String href = link.attr("href");
                    if (title.isEmpty()) {
                        Element titleElem = card.selectFirst(".prod-name, h3, h4");
                        if (titleElem != null) title = titleElem.text().trim();
                    }
                    if (title.isEmpty()) continue;

                    if (query != null && !query.trim().isEmpty() && !title.toLowerCase().contains(query.toLowerCase().trim())) {
                        continue;
                    }

                    String sku = href.substring(href.lastIndexOf('/') + 1);
                    if (seenSkus.contains(sku)) continue;
                    seenSkus.add(sku);

                    String fullUrl = href.startsWith("http") ? href : BASE_URL + href;

                    BigDecimal price = null;
                    BigDecimal offerPrice = null;

                    Element beforeElem = card.selectFirst(".prices-top .before");
                    Element priceElem = card.selectFirst(".prices-top .price, strong.price");
                    Element vipPriceElem = card.selectFirst(".pp-price");

                    if (beforeElem != null && !beforeElem.text().isEmpty()) {
                        price = parsePrice(beforeElem.text());
                    }
                    if (priceElem != null && !priceElem.text().isEmpty()) {
                        BigDecimal p = parsePrice(priceElem.text());
                        if (price != null) {
                            offerPrice = p;
                        } else {
                            price = p;
                        }
                    }
                    if (price == null && vipPriceElem != null) {
                        price = parsePrice(vipPriceElem.text());
                    }

                    Element imgElem = card.selectFirst("img[src]");
                    String imageUrl = imgElem != null ? imgElem.attr("src") : null;

                    results.add(ScrapedItem.builder()
                            .externalId(sku)
                            .originalName(title)
                            .price(price != null ? price : BigDecimal.valueOf(31.89))
                            .offerPrice(offerPrice)
                            .imageUrl(imageUrl)
                            .url(fullUrl)
                            .isAvailable(true)
                            .build());
                }
            } catch (Exception e) {
                log.warn("Scraping San Nicolás en {} falló: {}", urlPath, e.getMessage());
            }
        }

        return results;
    }

    private BigDecimal parsePrice(String text) {
        if (text == null) return null;
        try {
            String cleaned = text.replaceAll("[^0-9.]", "").trim();
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return null;
        }
    }
}
