package su26.uml.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.ProviderRate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRateRepository extends JpaRepository<ProviderRate, UUID> {

    Optional<ProviderRate> findByProviderAndModelName(String provider, String modelName);

    List<ProviderRate> findByIsActiveTrue();

    List<ProviderRate> findByProvider(String provider);
}
