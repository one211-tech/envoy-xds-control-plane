package com.one211.xds.auth;

import com.one211.xds.config.ConfigLoader;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for JWT token generation and validation
 */
public class JwtUtility {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtility.class);

    private static SecretKey key;
    private static long EXPIRATION_TIME;

    static {
        try {
            ConfigLoader.loadConfig();
            String secretKey = ConfigLoader.getJwtSecretKey();

            // Fail-fast if no secure secret is provided (in production)
            if (secretKey == null || secretKey.isEmpty() ||
                secretKey.equals("your-256-bit-secret-key-for-jwt-token-generation-change-this-in-production") ||
                secretKey.equals("VGhpcyBpcyBhIDY0IGJpdCBsb25nIGtleSB3aGljaCBzaG91bGQgYmUgY2hhbmdlZCBpbiBwcm9kdWN0aW9uLiBTbyBjaGFuZ2UgbWUgYW5kIG1ha2Ugc3VyZSBpdHMgMTI4IGJpdCBsb25nIG9yIG1vcmU")) {

                String errorMsg = "JWT_SECRET_KEY environment variable must be set with a secure 256-bit secret key. " +
                                  "Using environment variable DISABLE_AUTH=true is not allowed in production for security reasons.";
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            EXPIRATION_TIME = ConfigLoader.getJwtExpirationMs();
            key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            logger.info("JWT utility initialized with expiration time: {}ms", EXPIRATION_TIME);
        } catch (Exception e) {
            logger.error("Failed to initialize JWT utility, using fallback", e);
            // Fallback values - only for development with explicit DISABLE_AUTH=true
            String disableAuth = System.getenv().getOrDefault("DISABLE_AUTH", "false");
            if (Boolean.parseBoolean(disableAuth)) {
                String fallbackSecret = "dev-insecure-secret-key-do-not-use-in-production-" + UUID.randomUUID().toString().substring(0, 32);
                key = Keys.hmacShaKeyFor(fallbackSecret.getBytes(StandardCharsets.UTF_8));
                EXPIRATION_TIME = 24 * 60 * 60 * 1000;
                logger.warn("Using INSECURE fallback JWT secret key - DISABLE_AUTH is enabled. Set JWT_SECRET_KEY for production!");
            } else {
                throw new IllegalStateException("Failed to initialize JWT: configuration error and DISABLE_AUTH not set. " +
                                  "Set either JWT_SECRET_KEY environment variable or DISABLE_AUTH=true for development.");
            }
        }
    }

    /**
     * Generate a JWT token for the given subject (username)
     *
     * @param username the username to include in the token
     * @param role the role to include in the token
     * @return the generated JWT token
     */
    public static String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("username", username);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validate a JWT token
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract username from a JWT token
     *
     * @param token the JWT token
     * @return the username from the token
     */
    public static String extractUsername(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            logger.error("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract role from a JWT token
     *
     * @param token the JWT token
     * @return the role from the token, or null if not found
     */
    public static String extractRole(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("role", String.class);
        } catch (Exception e) {
            logger.error("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if a token is expired
     *
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    public static boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Extract all claims from a JWT token
     *
     * @param token the JWT token
     * @return the claims from the token, or null if extraction fails
     */
    public static Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            logger.error("Failed to extract claims from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the token expiration time
     *
     * @return the expiration time in milliseconds
     */
    public static long getExpirationTime() {
        return EXPIRATION_TIME;
    }

    /**
     * Check if running in production environment (not in development mode)
     *
     * @return true if production environment, false if development mode
     */
    public static boolean isProductionEnvironment() {
        String disableAuth = System.getenv().getOrDefault("DISABLE_AUTH", "false");
        return !Boolean.parseBoolean(disableAuth);
    }
}
