package com.luppo.farmacia.scraper.model;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapedItem {
    private String externalId;
    private String originalName;
    private String description;
    private String brand;
    private String presentation;
    private BigDecimal price;
    private BigDecimal offerPrice;
    private Boolean isAvailable;
    private String url;
    private String imageUrl;
    private String rawData;
}
