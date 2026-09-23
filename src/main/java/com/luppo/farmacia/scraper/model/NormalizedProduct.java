package com.luppo.farmacia.scraper.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NormalizedProduct {
    private String name;
    private String activeIngredient;
    private String concentration;
    private String pharmaceuticalForm;
    private String brand;
    private Integer quantity;
    private String unit;
    private Double confidence;
}
