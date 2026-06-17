package com.gym.security;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            new PublicEndpoint(HttpMethod.POST, "/trainees"),
            new PublicEndpoint(HttpMethod.POST, "/trainers"),
            new PublicEndpoint(HttpMethod.POST, "/auth/login"),
            new PublicEndpoint(HttpMethod.GET, "/api/training-types")
    );

    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        final String path = (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath))
                ? requestUri.substring(contextPath.length())
                : requestUri;

        boolean isPublic = PUBLIC_ENDPOINTS.stream().anyMatch(endpoint ->
                endpoint.method().matches(request.getMethod()) && PATH_MATCHER.match(endpoint.pathPattern(), path));

        if (isPublic) {
            log.debug("Skipping authentication for public endpoint: {} {}", request.getMethod(), path);
        }
        return isPublic;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Optional<Credentials> credentials = extractBasicAuthCredentials(request);

        if (credentials.isEmpty()) {
            log.warn("Missing or malformed Authorization header for {} {}", request.getMethod(), request.getRequestURI());
            rejectUnauthorized(response, "Basic");
            return;
        }

        String username = credentials.get().username();
        String password = credentials.get().password();

        if (!isValidUser(username, password)) {
            log.warn("Authentication failed for username: {}", username);
            rejectUnauthorized(response, "Basic");
            return;
        }

        log.debug("Authentication succeeded for username: {}", username);
        filterChain.doFilter(request, response);
    }

    private boolean isValidUser(String username, String password) {
        boolean isTrainee = traineeDao.findById(username)
                .map(t -> t.getPassword().equals(password))
                .orElse(false);

        if (isTrainee) {
            return true;
        }

        return trainerDao.findById(username)
                .map(t -> t.getPassword().equals(password))
                .orElse(false);
    }

    private Optional<Credentials> extractBasicAuthCredentials(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return Optional.empty();
        }

        String base64Credentials = header.substring(6).trim();
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            log.warn("Authorization header was not valid Base64: {}", ex.getMessage());
            return Optional.empty();
        }

        int separatorIndex = decoded.indexOf(':');
        if (separatorIndex < 0) {
            return Optional.empty();
        }

        String username = decoded.substring(0, separatorIndex);
        String password = decoded.substring(separatorIndex + 1);

        if (username.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new Credentials(username, password));
    }

    private void rejectUnauthorized(HttpServletResponse response, String scheme) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, scheme + " realm=\"gym-management-service\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing credentials\"}");
    }

    private record Credentials(String username, String password) {
    }

    private record PublicEndpoint(HttpMethod method, String pathPattern) {
    }
}