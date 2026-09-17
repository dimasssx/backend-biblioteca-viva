package org.bibliotecaviva.backend.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.bibliotecaviva.backend.domain.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JwtAuthenticationIntegrationTest extends IntegrationTestSupport {

    private static final String PROTECTED_PATH = "/work/liked";

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Test
    void malformedJwtShouldReturnUnauthorizedApiError() throws Exception {
        assertInvalidTokenResponse("not-a-jwt");
    }

    @Test
    void emptyJwtShouldReturnUnauthorizedApiError() throws Exception {
        assertInvalidTokenResponse("");
    }

    @Test
    void expiredJwtShouldReturnUnauthorizedApiError() throws Exception {
        User user = createActiveStudent();
        Date now = new Date();
        String expiredToken = Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("sessionVersion", user.getSessionVersion())
                .issuedAt(new Date(now.getTime() - 2_000))
                .expiration(new Date(now.getTime() - 1_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .compact();

        assertInvalidTokenResponse(expiredToken);
    }

    @Test
    void tokenForDeletedUserShouldReturnUnauthorizedApiError() throws Exception {
        User user = createActiveStudent();
        String token = jwtService.generateToken(user);
        userRepository.delete(user);
        userRepository.flush();
        entityManager.clear();

        assertInvalidTokenResponse(token);
    }

    private void assertInvalidTokenResponse(String token) throws Exception {
        mockMvc.perform(get(PROTECTED_PATH)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token inválido ou expirado"))
                .andExpect(jsonPath("$.path").value(PROTECTED_PATH))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
