package com.luppo.farmacia.scraper.service.scraper;

import com.luppo.farmacia.scraper.model.ScrapedItem;
import java.util.List;

public interface PharmacyScraper {
    String getPharmacyCode();
    String getPharmacyName();
    List<ScrapedItem> search(String query);
}
