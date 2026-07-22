package su26.uml.be.features.ai.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import su26.uml.be.features.ai.entity.ProviderRate;
import su26.uml.be.features.ai.repository.ProviderRateRepository;
import su26.uml.be.features.ai.service.ProviderRateCacheService;
import su26.uml.be.features.admin.service.SseService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OpenRouterSyncService {

    static final String OPENROUTER_MODELS_URL = "https://openrouter.ai/api/v1/models";

    ProviderRateRepository providerRateRepository;
    ProviderRateCacheService providerRateCacheService;
    SseService sseService;
    ObjectMapper objectMapper;

    @NonFinal
    @Value("${openrouter.api-key:}")
    String openRouterApiKey;

    @Scheduled(cron = "0 0 0 * * *")
    public void syncRates() {
        log.info("OpenRouter sync started at {}", LocalDateTime.now());

        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
            log.warn("OPENROUTER_API_KEY not configured, skipping sync");
            return;
        }

        try {
            // Get all current models from OpenRouter
            WebClient client = WebClient.builder()
                    .baseUrl(OPENROUTER_MODELS_URL)
                    .defaultHeader("Authorization", "Bearer " + openRouterApiKey)
                    .build();

            String response = client.get()
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));

            if (response == null) {
                log.warn("OpenRouter returned null response");
                return;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                log.warn("OpenRouter response has no 'data' array");
                return;
            }

            // Load existing active rates for diff calculation
            List<ProviderRate> existingRates = providerRateRepository.findByIsActiveTrue();
            Map<String, ProviderRate> existingMap = new HashMap<>();
            for (ProviderRate r : existingRates) {
                existingMap.put(r.getProvider() + ":" + r.getModelName(), r);
            }

            List<String> changes = new ArrayList<>();
            int upserted = 0;
            int noChange = 0;

            for (JsonNode model : data) {
                String modelId = model.get("id").asText();
                JsonNode pricing = model.get("pricing");
                if (pricing == null) continue;

                // Parse pricing (OpenRouter returns per-token, not per-1K)
                BigDecimal prompt = parsePrice(pricing.get("prompt"));
                BigDecimal completion = parsePrice(pricing.get("completion"));

                if (prompt == null || completion == null) continue;

                // Convert to per-1K tokens
                BigDecimal rateIn = prompt.multiply(BigDecimal.valueOf(1000));
                BigDecimal rateOut = completion.multiply(BigDecimal.valueOf(1000));

                // Extract provider prefix (e.g. "openai/gpt-4" → provider="openai", model="gpt-4")
                String provider = "openrouter";
                String modelName = modelId;
                int slashIdx = modelId.indexOf('/');
                if (slashIdx > 0) {
                    provider = modelId.substring(0, slashIdx);
                    modelName = modelId.substring(slashIdx + 1);
                }

                // Filter: only UPSERT models that exist in our provider_rates table
                if (!existingMap.containsKey(provider + ":" + modelName)) {
                    continue;
                }

                ProviderRate existing = existingMap.get(provider + ":" + modelName);
                boolean hasChanged = existing.getRateInPer1k().compareTo(rateIn) != 0
                        || existing.getRateOutPer1k().compareTo(rateOut) != 0;

                if (hasChanged) {
                    changes.add(String.format("[%s: in $%.6f->$%.6f, out $%.6f->$%.6f]",
                            modelId,
                            existing.getRateInPer1k(), rateIn,
                            existing.getRateOutPer1k(), rateOut));

                    existing.setRateInPer1k(rateIn);
                    existing.setRateOutPer1k(rateOut);
                    existing.setUpdatedAt(LocalDateTime.now());
                    providerRateRepository.save(existing);
                    providerRateCacheService.refreshOne(existing);
                    upserted++;
                } else {
                    noChange++;
                }
            }

            // Gửi SSE notification nếu có thay đổi
            String summary;
            if (changes.isEmpty()) {
                summary = "OpenRouter Sync Complete. No price changes detected (" + noChange + " models checked).";
            } else {
                summary = "OpenRouter Sync Complete. Changes: " + String.join(", ", changes)
                        + ". (" + upserted + " updated, " + noChange + " unchanged)";
            }
            log.info(summary);

            Map<String, Object> ssePayload = new HashMap<>();
            ssePayload.put("event", "openrouter_sync");
            ssePayload.put("summary", summary);
            ssePayload.put("modelsChecked", noChange + upserted);
            ssePayload.put("modelsUpdated", upserted);
            sseService.broadcast("openrouter_sync", ssePayload);

        } catch (Exception e) {
            log.error("OpenRouter sync failed", e);
        }
    }

    private BigDecimal parsePrice(JsonNode node) {
        if (node == null || node.isNull()) return null;
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}