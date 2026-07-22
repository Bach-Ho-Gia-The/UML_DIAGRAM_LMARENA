//package su26.uml.be.repository;
//*/
//package su26.uml.be.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import su26.uml.be.entity.RefreshToken;
//
//import java.time.Instant;
//import java.util.Optional;
//import java.util.UUID;
//
///*
//@Repository
//public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
//    Optional<RefreshToken> findByTokenHash(String tokenHash);
//
//    @Modifying
//    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.familyId = :familyId")
//    void revokeFamily(UUID familyId);
//
//    @Modifying
//    @Query("DELETE FROM RefreshToken r WHERE r.expiryTime < :now")
//    int deleteExpired(Instant now);
//}
//*/