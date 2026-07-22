package su26.uml.be.features.auth.service;

import com.nimbusds.jose.JOSEException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import su26.uml.be.features.auth.dto.IntrospectRequest;
import su26.uml.be.features.auth.dto.LoginRequest;
import su26.uml.be.features.auth.dto.LogoutRequest;
import su26.uml.be.features.auth.dto.AuthenticationResponse;
import su26.uml.be.features.auth.dto.IntrospectResponse;
import su26.uml.be.features.user.entity.User;

import java.text.ParseException;

public interface AuthenticationService {
    AuthenticationResponse authenticate(LoginRequest request, HttpServletResponse response);
    IntrospectResponse introspect(IntrospectRequest request);
    void logout(LogoutRequest request, HttpServletRequest httpRequest, HttpServletResponse response)
            throws ParseException, JOSEException;
    AuthenticationResponse refreshToken(HttpServletRequest httpRequest, HttpServletResponse response);
    AuthenticationResponse generateTokenForOAuth2User(User user);
    boolean isAccountLocked(String identifier);
}