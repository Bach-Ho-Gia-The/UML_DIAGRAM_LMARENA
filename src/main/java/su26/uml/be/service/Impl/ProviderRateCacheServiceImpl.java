package su26.uml.be.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import su26.uml.be.entity.ProviderRate;
import su26.uml.be.repository.ProviderRateRepository;
import su26.uml.be.service.ProviderRateCacheService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProviderRateCacheServiceImpl implements ProviderRateCacheService {

    static final long CACHE_TTL_SECONDS = 3600;

    StringRedisTemplate redisTemplate;
    ObjectMapper objectMapper;
    ProviderRateRepository providerRateRepository;

    @Override
    public Optional<ProviderRate> getRate(String provider, String modelName) {
        String key = redisKey(provider, modelName);
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            try {
                return Optional.of(objectMapper.readValue(json, ProviderRate.class));
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse cached ProviderRate for {}/{}", provider, modelName, e);
            }
        }
        // Cache miss → load from DB and cache
        Optional<ProviderRate> rate = providerRateRepository.findByProviderAndModelName(provider, modelName);
        rate.ifPresent(this::refreshOne);
        return rate;
    }

    @Override
    public void refreshAll() {
        List<ProviderRate> rates = providerRateRepository.findByIsActiveTrue();
        rates.forEach(this::refreshOne);
        log.info("ProviderRate cache refreshed: {} active rates cached", rates.size());
    }

    @Override
    public void refreshOne(ProviderRate rate) {
        try {
            String json = objectMapper.writeValueAsString(rate);
            redisTemplate.opsForValue().set(redisKey(rate.getProvider(), rate.getModelName()), json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ProviderRate for {}/{}", rate.getProvider(), rate.getModelName(), e);
        }
    }

    private String redisKey(String provider, String modelName) {
        return "provider_rate:" + provider + ":" + modelName;
    }
}
