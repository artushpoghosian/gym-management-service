package com.gym.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private GymUserDetailsService userDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    private static final String TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/trainees/some.user");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void noAuthorizationHeader_ChainContinuesUnauthenticated() throws ServletException, IOException {
        filter.doFilter(request, response, chain);

        assertThat(currentAuth()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verify(jwtService, never()).isValid(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void nonBearerHeader_ChainContinuesUnauthenticated() throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertThat(currentAuth()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void invalidToken_NoAuthenticationSet() throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtService.isValid(TOKEN)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(currentAuth()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void blacklistedToken_NoAuthenticationSet() throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtService.isValid(TOKEN)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(TOKEN)).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertThat(currentAuth()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void validToken_PopulatesSecurityContext() throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtService.isValid(TOKEN)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(TOKEN)).thenReturn(false);
        when(jwtService.extractUsername(TOKEN)).thenReturn("alice.brown");
        UserDetails ud = User.withUsername("alice.brown").password("").roles("TRAINEE").build();
        when(userDetailsService.loadUserByUsername("alice.brown")).thenReturn(ud);

        filter.doFilter(request, response, chain);

        assertThat(currentAuth()).isNotNull();
        assertThat(currentAuth().getName()).isEqualTo("alice.brown");
        assertThat(currentAuth().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_TRAINEE");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void validToken_WhenContextAlreadyAuthenticated_DoesNotOverwrite() throws ServletException, IOException {
        Authentication existing = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "existing.user", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);

        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtService.isValid(TOKEN)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(TOKEN)).thenReturn(false);
        when(jwtService.extractUsername(TOKEN)).thenReturn("alice.brown");

        filter.doFilter(request, response, chain);

        assertThat(currentAuth()).isSameAs(existing);
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }
}
