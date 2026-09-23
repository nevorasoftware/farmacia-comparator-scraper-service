package com.luppo.farmacia.scraper.service.ai;

import com.luppo.farmacia.scraper.model.NormalizedProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ProductNormalizer {

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    private final GeminiAiProvider geminiAiProvider;

    public ProductNormalizer(GeminiAiProvider geminiAiProvider) {
        this.geminiAiProvider = geminiAiProvider;
    }

    public NormalizedProduct normalize(String rawName, String rawDescription) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return NormalizedProduct.builder().name("Desconocido").confidence(0.0).build();
        }

        // Try AI if enabled and API key configured
        if (aiEnabled && geminiAiProvider.isAvailable()) {
            try {
                NormalizedProduct aiResult = geminiAiProvider.normalize(rawName, rawDescription);
                if (aiResult != null && aiResult.getConfidence() != null && aiResult.getConfidence() > 0.6) {
                    return aiResult;
                }
            } catch (Exception e) {
                log.warn("AI normalization error, falling back to deterministic heuristic: {}", e.getMessage());
            }
        }

        // Deterministic heuristic normalizer
        return normalizeDeterministic(rawName);
    }

    public NormalizedProduct normalizeDeterministic(String text) {
        String upper = text.toUpperCase();
        String activeIng = extractActiveIngredient(upper);
        String concentration = extractConcentration(upper);
        String form = extractPharmaceuticalForm(upper);
        String brand = extractBrand(upper);
        int qty = extractQuantity(upper);

        return NormalizedProduct.builder()
                .name(capitalize(activeIng != null ? activeIng : text))
                .activeIngredient(activeIng != null ? activeIng : "No especificado")
                .concentration(concentration)
                .pharmaceuticalForm(form)
                .brand(brand)
                .quantity(qty > 0 ? qty : 1)
                .unit(form.toLowerCase())
                .confidence(0.85)
                .build();
    }

    private String extractActiveIngredient(String text) {
        if (text.contains("ACETAMINOFEN") || text.contains("PARACETAMOL")) return "Acetaminofén";
        if (text.contains("IBUPROFENO") || text.contains("ADVIL")) return "Ibuprofeno";
        if (text.contains("AMOXICILINA")) return "Amoxicilina";
        if (text.contains("LORATADINA")) return "Loratadina";
        if (text.contains("METFORMINA")) return "Metformina";
        if (text.contains("LOSARTAN") || text.contains("LOSARTÁN")) return "Losartán Potásico";
        if (text.contains("OMEPRAZOL")) return "Omeprazol";
        if (text.contains("AZITROMICINA")) return "Azitromicina";
        if (text.contains("CIPROFLOXACINA")) return "Ciprofloxacina";
        return null;
    }

    private String extractConcentration(String text) {
        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?\\s*(MG|G|MCG|ML|%|UI)(/\\d*\\s*ML)?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractPharmaceuticalForm(String text) {
        if (text.contains("JARABE") || text.contains("JBE")) return "Jarabe";
        if (text.contains("CAPSULA") || text.contains("CÁPSULA") || text.contains("CAP")) return "Cápsula";
        if (text.contains("TABLETA") || text.contains("TAB") || text.contains("COMPRIMIDO")) return "Tableta";
        if (text.contains("SUSPENSION") || text.contains("SUSPENSIÓN")) return "Suspensión";
        if (text.contains("GOTAS")) return "Gotas";
        if (text.contains("CREMA")) return "Crema";
        if (text.contains("GEL")) return "Gel";
        if (text.contains("INYECTABLE") || text.contains("AMPOLLA")) return "Inyectable";
        return "Tableta";
    }

    private String extractBrand(String text) {
        if (text.contains("MK")) return "MK";
        if (text.contains("ECOMED")) return "Ecomed";
        if (text.contains("LA SANTE")) return "La Santé";
        if (text.contains("VIJOSA")) return "Vijosa";
        if (text.contains("LAB.SUIZOS") || text.contains("SUIZOS")) return "Laboratorios Suizos";
        if (text.contains("BAYER")) return "Bayer";
        if (text.contains("PFIZER")) return "Pfizer";
        if (text.contains("ADVIL")) return "Advil";
        if (text.contains("TYLENOL")) return "Tylenol";
        return "Genérico / Comercial";
    }

    private int extractQuantity(String text) {
        Pattern pattern = Pattern.compile("X\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
