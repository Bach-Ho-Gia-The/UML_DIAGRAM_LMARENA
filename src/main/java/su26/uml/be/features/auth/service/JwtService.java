package su26.uml.be.features.auth.service;

import java.text.ParseException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;

import su26.uml.be.features.user.entity.User;

public interface JwtService {
    String generateAccessToken(User user);

    String generateRefreshToken(User user, String jti);

    JWTClaimsSet parseClaims(String token) throws JOSEException, ParseException;
}