package io.aegis.spring.security;

import io.aegis.core.AuthenticationManager;
import io.aegis.core.context.*;
import io.aegis.core.result.AuthenticationResult;
import io.aegis.core.user.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Spring Security filter that integrates Aegis authentication.
 * <p>
 * Extracts authentication context from HTTP requests and delegates to
 * the AuthenticationManager for validation.
 *
 * @since 1.0.0
 */
public class AegisSecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AegisSecurityFilter.class);

    private final AuthenticationManager authenticationManager;

    public AegisSecurityFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract authentication context from request
            AuthenticationContext context = extractAuthenticationContext(request);

            if (context != null) {
                // Authenticate
                AuthenticationResult result = authenticationManager.authenticate(context);

                if (result.isSuccess() && result.getPrincipal().isPresent()) {
                    UserPrincipal principal = result.getPrincipal().get();

                    // Convert to Spring Security authentication
                    var authorities = principal.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            authorities
                    );

                    // Set authentication in Spring Security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    logger.debug("Authentication successful for user: {}", principal.getUsername());
                }
            }

        } catch (Exception e) {
            logger.error("Authentication error", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts authentication context from the HTTP request.
     * Tries multiple authentication mechanisms in order.
     */
    private AuthenticationContext extractAuthenticationContext(HttpServletRequest request) {
        // Try JWT (Bearer token)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return new JwtContext(token, "header");
        }

        // Try API Key
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isEmpty()) {
            return new ApiKeyContext(apiKey, "X-API-Key", "header");
        }

        // Try Basic Authentication
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            String base64Credentials = authHeader.substring(6);
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));
            String[] parts = credentials.split(":", 2);
            if (parts.length == 2) {
                return new BasicAuthContext(parts[0], parts[1], "header");
            }
        }

        // Try API Key from query parameter
        String apiKeyParam = request.getParameter("api_key");
        if (apiKeyParam != null && !apiKeyParam.isEmpty()) {
            return new ApiKeyContext(apiKeyParam, "api_key", "query");
        }

        return null;
    }
}
