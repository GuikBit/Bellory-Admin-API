package org.exemplo.belloryadmin.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.exemplo.belloryadmin.model.entity.users.UsuarioAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * TokenService do Bellory-Admin-API.
 *
 * <p>Compativel com o TokenService do Bellory-API: mesmo issuer, mesmo segredo,
 * mesmas claims. A diferenca e que aqui so emitimos tokens com {@code userType=PLATFORM_ADMIN}.
 * Tokens APP do cliente nao passam por aqui.</p>
 */
@Service
public class TokenService {

    @Value("${api.security.token.secret:my-secret-key}")
    private String secret;

    @Value("${api.security.token.expiration:36000}")
    private Long expirationTime;

    /**
     * Gera token JWT para usuarios admin da plataforma (sem organizacao).
     */
    public String generateToken(UsuarioAdmin admin) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("bellory-api")
                    .withSubject(admin.getUsername())
                    .withClaim("userId", admin.getId())
                    .withClaim("role", admin.getRole())
                    .withClaim("nomeCompleto", admin.getNomeCompleto())
                    .withClaim("userType", "PLATFORM_ADMIN")
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("bellory-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("bellory-api")
                    .build()
                    .verify(token)
                    .getClaim("userId")
                    .asLong();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("bellory-api")
                    .build()
                    .verify(token)
                    .getClaim("role")
                    .asString();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    /**
     * Tipo do usuario do token. Tokens antigos sem userType caem em "APP" por
     * retrocompatibilidade do TokenService original; aqui o admin so reconhece
     * "PLATFORM_ADMIN" como valido (rejeitado fora dele no JwtAuthFilter).
     */
    public String getUserTypeFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String userType = JWT.require(algorithm)
                    .withIssuer("bellory-api")
                    .build()
                    .verify(token)
                    .getClaim("userType")
                    .asString();
            return userType != null ? userType : "APP";
        } catch (JWTVerificationException exception) {
            return "APP";
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            Instant expiration = JWT.require(algorithm)
                    .withIssuer("bellory-api")
                    .build()
                    .verify(token)
                    .getExpiresAt()
                    .toInstant();

            return expiration.isBefore(Instant.now());
        } catch (JWTVerificationException exception) {
            return true;
        }
    }

    /**
     * Renova um token PLATFORM_ADMIN ainda valido.
     */
    public String refreshToken(String token) {
        try {
            String username = validateToken(token);
            if (username == null || username.isEmpty()) {
                throw new RuntimeException("Token invalido");
            }

            Long userId = getUserIdFromToken(token);
            String role = getRoleFromToken(token);
            String userType = getUserTypeFromToken(token);

            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("bellory-api")
                    .withSubject(username)
                    .withClaim("userId", userId)
                    .withClaim("role", role)
                    .withClaim("userType", userType)
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao renovar token", exception);
        }
    }

    public LocalDateTime getExpirationDateTime() {
        return LocalDateTime.ofInstant(genExpirationDate(), ZoneId.of("America/Sao_Paulo"));
    }

    private Instant genExpirationDate() {
        return Instant.now().plusSeconds(expirationTime);
    }
}
