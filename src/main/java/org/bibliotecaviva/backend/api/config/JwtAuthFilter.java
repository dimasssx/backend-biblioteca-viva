package org.bibliotecaviva.backend.api.config;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.bibliotecaviva.backend.application.services.JwtService;
import org.bibliotecaviva.backend.domain.exceptions.InvalidJwtAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String INVALID_TOKEN_MESSAGE = "Token inválido ou expirado";
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        if (jwt.isBlank()) {
            rejectRequest(request, response, null);
            return;
        }

        try {
            final String userEmail = jwtService.extractUsername(jwt);
            if (userEmail == null || userEmail.isBlank()) {
                rejectRequest(request, response, null);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                if (!jwtService.isTokenValid(jwt, userDetails)) {
                    rejectRequest(request, response, null);
                    return;
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException | UsernameNotFoundException ex) {
            rejectRequest(request, response, ex);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void rejectRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            RuntimeException cause
    ) throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        InvalidJwtAuthenticationException exception = cause == null
                ? new InvalidJwtAuthenticationException(INVALID_TOKEN_MESSAGE)
                : new InvalidJwtAuthenticationException(INVALID_TOKEN_MESSAGE, cause);
        authenticationEntryPoint.commence(request, response, exception);
    }
}
