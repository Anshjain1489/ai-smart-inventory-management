package com.anshtechsolution.smart_inventory.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0LTAx",
        "app.security.jwt.issuer=smart-inventory-test"
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    // Generate a real JWT for the requested role.
    private String generateToken(String role) {

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "integration-user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        return jwtService.generateToken(authentication);
    }

    private String bearerToken(String role) {
        return "Bearer " + generateToken(role);
    }

    @Test
    void protectedEndpointShouldRejectUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicLoginEndpointShouldBeAccessibleWithoutToken()
            throws Exception {

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicSignupEndpointShouldBeAccessibleWithoutToken()
            throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminShouldAccessProductsEndpoint()
            throws Exception {

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", bearerToken("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void managerShouldAccessProductsEndpoint()
            throws Exception {

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", bearerToken("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void viewerShouldAccessProductsEndpoint()
            throws Exception {

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", bearerToken("VIEWER")))
                .andExpect(status().isOk());
    }

    @Test
    void viewerShouldNotCreateProducts()
            throws Exception {

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", bearerToken("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerShouldNotUpdateProducts()
            throws Exception {

        mockMvc.perform(put("/api/v1/products/1")
                        .header("Authorization", bearerToken("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerShouldNotDeleteProducts()
            throws Exception {

        mockMvc.perform(delete("/api/v1/products/1")
                        .header("Authorization", bearerToken("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerShouldNotAccessAiEndpoints()
            throws Exception {

        mockMvc.perform(get("/api/v1/ai/insights")
                        .header("Authorization", bearerToken("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidJwtShouldBeRejected()
            throws Exception {

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unconfiguredEndpointShouldBeDenied() throws Exception {
        mockMvc.perform(get("/api/v1/not-configured"))
                .andExpect(status().isUnauthorized());
    }
}