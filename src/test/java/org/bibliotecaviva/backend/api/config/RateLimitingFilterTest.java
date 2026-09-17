package org.bibliotecaviva.backend.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.bibliotecaviva.backend.application.services.RateLimiterService;
import org.bibliotecaviva.backend.persistence.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RateLimitingFilter}.
 *
 * <p>The integration suite disables rate limiting via {@code security.rate-limit.enabled=false}
 * (correct — avoids flaky parallel test failures). These unit tests cover the filter's branching
 * logic in isolation so the code is not invisible to JaCoCo.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(rateLimiterService, refreshTokenRepository);
        // Inject @Value fields via ReflectionTestUtils (no Spring context needed)
        ReflectionTestUtils.setField(filter, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(filter, "loginIpLimit", 5);
        ReflectionTestUtils.setField(filter, "refreshTokenLimit", 3);
        ReflectionTestUtils.setField(filter, "refreshIpLimit", 10);
        ReflectionTestUtils.setField(filter, "registerIpLimit", 5);
        ReflectionTestUtils.setField(filter, "passwordResetIpLimit", 3);
        ReflectionTestUtils.setField(filter, "windowMillis", 60_000L);
    }

    // -------------------------------------------------------------------------
    // Disabled flag — must bypass all checks
    // -------------------------------------------------------------------------

    @Test
    void whenDisabledAllRequestsPassThrough() throws Exception {
        ReflectionTestUtils.setField(filter, "rateLimitEnabled", false);
        MockHttpServletRequest req = postRequest("/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
        verify(rateLimiterService, never()).isAllowed(anyString(), anyInt(), anyLong());
    }

    // -------------------------------------------------------------------------
    // Login endpoint
    // -------------------------------------------------------------------------

    @Test
    void loginWithinLimitPassesThrough() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(true);
        MockHttpServletRequest req = postRequest("/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
    }

    @Test
    void loginOverLimitReturns429() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(false);
        when(rateLimiterService.getRetryAfterSeconds(anyString(), anyLong())).thenReturn(30L);

        MockHttpServletRequest req = postRequest("/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, filterChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), res.getStatus());
        assertEquals("30", res.getHeader("Retry-After"));
        verify(filterChain, never()).doFilter(req, res);

        // Response body must be valid JSON with status 429
        var body = new ObjectMapper().readTree(res.getContentAsString());
        assertEquals(429, body.get("status").asInt());
    }

    // -------------------------------------------------------------------------
    // Register endpoint
    // -------------------------------------------------------------------------

    @Test
    void registerOverLimitReturns429() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(false);
        when(rateLimiterService.getRetryAfterSeconds(anyString(), anyLong())).thenReturn(60L);

        MockHttpServletRequest req = postRequest("/auth/register");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, filterChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), res.getStatus());
        verify(filterChain, never()).doFilter(req, res);
    }

    // -------------------------------------------------------------------------
    // Password-reset endpoint
    // -------------------------------------------------------------------------

    @Test
    void passwordResetOverLimitReturns429() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(false);
        when(rateLimiterService.getRetryAfterSeconds(anyString(), anyLong())).thenReturn(60L);

        MockHttpServletRequest req = postRequest("/auth/password-reset/initiate");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, filterChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), res.getStatus());
    }

    // -------------------------------------------------------------------------
    // Refresh endpoint — with and without cookie
    // -------------------------------------------------------------------------

    @Test
    void refreshWithValidCookieChecksTokenAndIpLimits() throws Exception {
        UUID familyId = UUID.randomUUID();
        when(refreshTokenRepository.findFamilyIdByTokenHash(anyString())).thenReturn(Optional.of(familyId));
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(true);

        MockHttpServletRequest req = postRequest("/auth/refresh");
        req.setCookies(new jakarta.servlet.http.Cookie("refreshToken", "raw-token-value"));
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
    }

    @Test
    void refreshTokenLimitExceededReturns429() throws Exception {
        UUID familyId = UUID.randomUUID();
        when(refreshTokenRepository.findFamilyIdByTokenHash(anyString())).thenReturn(Optional.of(familyId));
        // First isAllowed call (token-scoped) returns false → short-circuit, 429
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(false);
        when(rateLimiterService.getRetryAfterSeconds(anyString(), anyLong())).thenReturn(30L);

        MockHttpServletRequest req = postRequest("/auth/refresh");
        req.setCookies(new jakarta.servlet.http.Cookie("refreshToken", "raw-token-value"));
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, filterChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), res.getStatus());
        verify(filterChain, never()).doFilter(req, res);
    }

    @Test
    void refreshWithNoCookieStillAppliesIpLimit() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(true);

        MockHttpServletRequest req = postRequest("/auth/refresh");
        // No cookie set
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
        // Token-scoped limit should NOT have been checked (no cookie)
        verify(refreshTokenRepository, never()).findFamilyIdByTokenHash(anyString());
    }

    // -------------------------------------------------------------------------
    // Non-POST requests — must never trigger rate limiting
    // -------------------------------------------------------------------------

    @Test
    void getRequestPassesThroughWithoutRateLimitCheck() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
        verify(rateLimiterService, never()).isAllowed(anyString(), anyInt(), anyLong());
    }

    // -------------------------------------------------------------------------
    // IP extraction — X-Forwarded-For header
    // -------------------------------------------------------------------------

    @Test
    void xForwardedForHeaderIsUsedAsClientIp() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(true);

        MockHttpServletRequest req = postRequest("/auth/login");
        req.addHeader("X-Forwarded-For", "203.0.113.42, 10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, filterChain);

        // Verify the key used contains the first IP from the header
        verify(rateLimiterService).isAllowed(
                org.mockito.ArgumentMatchers.contains("203.0.113.42"),
                anyInt(), anyLong());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static MockHttpServletRequest postRequest(String uri) {
        return new MockHttpServletRequest("POST", uri);
    }
}
