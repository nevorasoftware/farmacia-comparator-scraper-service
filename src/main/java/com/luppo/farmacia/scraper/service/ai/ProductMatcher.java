package com.luppo.farmacia.scraper.service.ai;

import org.springframework.stereotype.Service;

@Service
public class ProductMatcher {

    public double computeSimilarity(String nameA, String nameB) {
        if (nameA == null || nameB == null) return 0.0;
        String a = nameA.trim().toLowerCase();
        String b = nameB.trim().toLowerCase();

        if (a.equals(b)) return 1.0;
        if (a.contains(b) || b.contains(a)) return 0.90;

        // Jaccard similarity of words
        String[] wordsA = a.split("\\s+");
        String[] wordsB = b.split("\\s+");
        int intersection = 0;
        for (String wA : wordsA) {
            for (String wB : wordsB) {
                if (wA.length() > 2 && wA.equals(wB)) {
                    intersection++;
                    break;
                }
            }
        }
        int total = wordsA.length + wordsB.length - intersection;
        return total > 0 ? (double) intersection / total : 0.0;
    }
}
