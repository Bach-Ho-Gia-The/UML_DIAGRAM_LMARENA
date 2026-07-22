package su26.uml.be.features.ai.service;

import su26.uml.be.features.ai.entity.ProviderRate;

import java.util.Optional;

public interface ProviderRateCacheService {

    Optional<ProviderRate> getRate(String provider, String modelName);

    void refreshAll();

    void refreshOne(ProviderRate rate);
}