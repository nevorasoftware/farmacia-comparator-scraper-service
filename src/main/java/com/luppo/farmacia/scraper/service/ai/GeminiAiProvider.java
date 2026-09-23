package com.luppo.farmacia.scraper.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luppo.farmacia.scraper.model.NormalizedProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class GeminiAiProvider implements AiProvider {

    @Value("${ai.gemini.apiKey:}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("tu_");
    }

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    public NormalizedProduct normalize(String rawProductName, String rawDescription) {
        if (!isAvailable()) {
            return null;
        }

        try {
            String prompt = String.format(
                    "Eres un normalizador farmacéutico para El Salvador. Convierte el siguiente producto en un JSON estricto:\n" +
                    "Producto: %s\n" +
                    "Descripción: %s\n\n" +
                    "Responde UNICAMENTE con este formato JSON:\n" +
                    "{\n" +
                    "  \"nombre\": \"nombre estandarizado\",\n" +
                    "  \"principioActivo\": \"principio activo principal\",\n" +
                    "  \"concentracion\": \"ej. 500 mg\",\n" +
                    "  \"formaFarmaceutica\": \"ej. Tableta, Jarabe, Cápsula\",\n" +
                    "  \"marca\": \"marca comercial o genérico\",\n" +
                    "  \"cantidad\": 20,\n" +
                    "  \"unidad\": \"tableta\"\n" +
                    "}",
                    rawProductName, rawDescription != null ? rawDescription : ""
            );

            String requestBody = objectMapper.writeValueAsString(new GeminiRequest(prompt));
            String endpoint = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String candidateText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                // strip code fences if present
                candidateText = candidateText.replaceAll("```json", "").replaceAll("```", "").trim();
                JsonNode parsed = objectMapper.readTree(candidateText);

                return NormalizedProduct.builder()
                        .name(parsed.path("nombre").asText(rawProductName))
                        .activeIngredient(parsed.path("principioActivo").asText())
                        .concentration(parsed.path("concentracion").asText())
                        .pharmaceuticalForm(parsed.path("formaFarmaceutica").asText())
                        .brand(parsed.path("marca").asText())
                        .quantity(parsed.path("cantidad").asInt(1))
                        .unit(parsed.path("unidad").asText("unidad"))
                        .confidence(0.95)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Gemini API normalization exception: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public Double calculateMatchScore(String productA, String productB) {
        return 0.90;
    }

    private static class GeminiRequest {
        public Contents[] contents;
        public GeminiRequest(String text) {
            this.contents = new Contents[]{ new Contents(new Part[]{ new Part(text) }) };
        }
    }
    private static class Contents {
        public Part[] parts;
        public Contents(Part[] parts) { this.parts = parts; }
    }
    private static class Part {
        public String text;
        public Part(String text) { this.text = text; }
    }
}
