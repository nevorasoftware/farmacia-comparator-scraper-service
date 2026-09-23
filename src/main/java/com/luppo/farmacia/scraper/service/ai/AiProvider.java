package com.luppo.farmacia.scraper.service.ai;

import com.luppo.farmacia.scraper.model.NormalizedProduct;

public interface AiProvider {
    String getProviderName();
    NormalizedProduct normalize(String rawProductName, String rawDescription);
    Double calculateMatchScore(String productA, String productB);
}
