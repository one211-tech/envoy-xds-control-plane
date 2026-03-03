package com.one211.xds.auth;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * HTTP filter for JWT authentication
 */
public class AuthenticationFilter extends Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final UserStore userStore;
    private final boolean requireAuthentication;

    public AuthenticationFilter(UserStore userStore, boolean requireAuthentication) {
        this.userStore = userStore;
        this.requireAuthentication = requireAuthentication;
    }

    public AuthenticationFilter(UserStore userStore) {
        this(userStore, true);
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        // Skip authentication for public endpoints
        String path = exchange.getRequestURI().getPath();
        if (AuthenticationHelper.isPublicEndpoint(path)) {
            logger.debug("Skipping authentication for public endpoint: {}", path);
            chain.doFilter(exchange);
            return;
        }

        // If authentication is disabled, allow all requests
        if (!requireAuthentication) {
            logger.debug("Authentication disabled, allowing request to: {}", path);
            chain.doFilter(exchange);
            return;
        }

        // Extract and validate JWT token
        String username = AuthenticationHelper.extractAndValidateToken(exchange);

        if (username != null) {
            // Add user information to request headers for downstream use
            AuthenticationHelper.addUserHeaders(exchange, username);

            // Check authorization for protected endpoints
            String requiredPermission = AuthorizationHelper.getRequiredPermission(path);
            if (requiredPermission != null && !userStore.hasPermission(username, requiredPermission)) {
                logger.warn("Authorization failed for user {} on endpoint {}: missing permission {}",
                        username, path, requiredPermission);
                AuthorizationHelper.sendForbiddenResponse(exchange);
                return;
            }

            logger.debug("User {} authenticated for endpoint: {}", username, path);
            chain.doFilter(exchange);
        } else {
            logger.warn("Authentication failed for endpoint: {}", path);
            AuthenticationHelper.sendUnauthorizedResponse(exchange);
        }
    }

    @Override
    public String description() {
        return "JWT Authentication Filter";
    }
}
