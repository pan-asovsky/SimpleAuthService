package dev.panasovsky.module.auth.components;

import dev.panasovsky.module.auth.model.User;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.time.ZoneId;
import java.time.Instant;
import java.security.Key;
import javax.crypto.SecretKey;
import java.time.LocalDateTime;


@Slf4j
@Component
public class JwtProvider {

    private final SecretKey jwtAccessSecret;
    private final SecretKey jwtRefreshSecret;


    public JwtProvider(
            @Value("${jwt.secret.access}") final String jwtAccessSecret,
            @Value("${jwt.secret.refresh}") final String jwtRefreshSecret)
    {
        this.jwtAccessSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtAccessSecret));
        this.jwtRefreshSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtRefreshSecret));
    }

    public String generateAccessToken(@NonNull final User user) {

        final LocalDateTime now = LocalDateTime.now();
        final Instant accessExpirationInstant = now.plusMinutes(5)
                .atZone(ZoneId.systemDefault()).toInstant();
        final Date accessExpiration = Date.from(accessExpirationInstant);

        return Jwts.builder()
                .setSubject(user.getLogin())
                .setId(user.getId().toString())
                .setExpiration(accessExpiration)
                .signWith(jwtAccessSecret)
                .claim("role", user.getUser_role().getRolename())
                .compact();
    }

    public String generateRefreshToken(@NonNull final User user) {

        final LocalDateTime now = LocalDateTime.now();
        final Instant refreshExpirationInstant = now.plusDays(10)
                .atZone(ZoneId.systemDefault()).toInstant();
        final Date refreshExpiration = Date.from(refreshExpirationInstant);

        return Jwts.builder()
                .setSubject(user.getLogin())
                .setId(user.getId().toString())
                .setExpiration(refreshExpiration)
                .signWith(jwtRefreshSecret)
                .compact();
    }

    public boolean validateAccessToken(@NonNull final String accessToken) {
        return validateToken(accessToken, jwtAccessSecret);
    }

    public boolean validateRefreshToken(@NonNull final String refreshToken) {
        return validateToken(refreshToken, jwtRefreshSecret);
    }

    private boolean validateToken(@NonNull final String token,
                                  @NonNull final Key secret) {

        try {
            Jwts.parserBuilder()
                    .setSigningKey(secret)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (final ExpiredJwtException expEx) {
            log.error("Token expired: ", expEx);
        } catch (final UnsupportedJwtException unsEx) {
            log.error("Unsupported JWT: ", unsEx);
        } catch (final MalformedJwtException mExc) {
            log.error("Malformed JWT: ", mExc);
        } catch (final SignatureException sExc) {
            log.error("Invalid signature: ", sExc);
        } catch (final Exception e) {
            log.error("Invalid token: ", e);
        }
        return false;
    }

    public Claims getAccessClaims(@NonNull final String token) {
        return getClaims(token, jwtAccessSecret);
    }

    public Claims getRefreshClaims(@NonNull final String token) {
        return getClaims(token, jwtRefreshSecret);
    }

    private Claims getClaims(@NonNull final String token, @NonNull final Key secret) {

        return Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}