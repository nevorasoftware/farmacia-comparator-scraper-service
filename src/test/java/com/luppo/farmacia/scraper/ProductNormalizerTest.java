package com.luppo.farmacia.scraper;

import com.luppo.farmacia.scraper.model.NormalizedProduct;
import com.luppo.farmacia.scraper.service.ai.GeminiAiProvider;
import com.luppo.farmacia.scraper.service.ai.ProductNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductNormalizerTest {

    @Mock
    private GeminiAiProvider geminiAiProvider;

    private ProductNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new ProductNormalizer(geminiAiProvider);
    }

    @Test
    void shouldNormalizeAcetaminofenCorrectly() {
        String raw = "ACETAMINOFEN MK 500MG X 20 TABLETAS";
        NormalizedProduct result = normalizer.normalizeDeterministic(raw);

        assertNotNull(result);
        assertEquals("Acetaminofén", result.getActiveIngredient());
        assertEquals("500MG", result.getConcentration());
        assertEquals("Tableta", result.getPharmaceuticalForm());
        assertEquals("MK", result.getBrand());
        assertEquals(20, result.getQuantity());
    }

    @Test
    void shouldNormalizeIbuprofenoJarabe() {
        String raw = "IBUPROFENO JARABE 100 MG/ 5 ML FRASCO X 120 ML";
        NormalizedProduct result = normalizer.normalizeDeterministic(raw);

        assertNotNull(result);
        assertEquals("Ibuprofeno", result.getActiveIngredient());
        assertEquals("Jarabe", result.getPharmaceuticalForm());
        assertEquals(120, result.getQuantity());
    }
}
