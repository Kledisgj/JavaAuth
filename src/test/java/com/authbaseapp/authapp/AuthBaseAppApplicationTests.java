package com.authbaseapp.authapp;

import com.authbaseapp.authapp.Config.RateLimitFilter;
import com.authbaseapp.authapp.Repository.UsersAppRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-tests;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.username=sa", "spring.datasource.password="
})
class AuthBaseAppApplicationTests {
    @LocalServerPort private int port;
    @Autowired private ObjectMapper mapper;
    @Autowired private UsersAppRepository repository;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void registrationLoginAndBearerAuthenticationWork() throws Exception {
        JsonNode user = request("POST", "/api/auth/register", registerBody(), null, 201).path("data");
        assertEquals("USER", user.path("role").asText());
        assertFalse(user.has("password"));
        assertNotEquals("Password123!", repository.findByEmail("user@example.com").orElseThrow().getPassword());

        JsonNode login = request("POST", "/api/auth/login",
                "{\"email\":\"USER@example.com\",\"password\":\"Password123!\"}", null, 200);
        String token = login.path("data").path("token").asText();
        assertFalse(token.isBlank());
        assertEquals("user@example.com", request("GET", "/api/auth/me", null, token, 200)
                .path("data").path("email").asText());
        request("GET", "/api/auth/me", null, null, 401);
        request("GET", "/api/auth/me", null, "invalid-token", 401);
    }

    @Test
    void wrongPasswordsAndDeletedAccountsCannotLogIn() throws Exception {
        request("POST", "/api/auth/register", registerBody(), null, 201);
        request("POST", "/api/auth/login",
                "{\"email\":\"user@example.com\",\"password\":\"WrongPassword!\"}", null, 401);
        request("POST", "/api/auth/login",
                "{\"email\":\"missing@example.com\",\"password\":\"Password123!\"}", null, 401);
        var user = repository.findByEmail("user@example.com").orElseThrow();
        user.setDelete(true);
        repository.save(user);
        request("POST", "/api/auth/login",
                "{\"email\":\"user@example.com\",\"password\":\"Password123!\"}", null, 401);
    }

    @Test
    void duplicateAndInvalidRequestsHaveConsistentErrors() throws Exception {
        request("POST", "/api/auth/register", registerBody(), null, 201);
        request("POST", "/api/auth/register", registerBody(), null, 409);
        request("POST", "/api/auth/register", "{}", null, 400);
        request("POST", "/api/auth/login", "{", null, 400);
        request("POST", "/api/auth/register",
                "{\"name\":\"User\",\"email\":\"invalid\",\"password\":\"Password123!\"}", null, 400);
    }

    @Test
    void frontendCorsPreflightWorksWithoutAuthentication() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri("/api/auth/login"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://127.0.0.1:5500")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type").build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("http://127.0.0.1:5500", response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
    }

    @Test
    void rateLimitReturnsValidApiResponseJson() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(mapper);
        MockHttpServletResponse response = null;
        for (int i = 0; i < 101; i++) {
            response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("GET", "/api/auth/me"), response, (req, res) -> { });
        }
        assertNotNull(response);
        assertEquals(429, response.getStatus());
        assertFalse(mapper.readTree(response.getContentAsString()).path("success").asBoolean());
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    void dtoAnnotationsRejectInvalidFieldsAndRespectBcryptByteLimit() throws Exception {
        JsonNode invalidName = request("POST", "/api/auth/register",
                registerBody().replace("\"User\"", "\" \""), null, 400);
        assertEquals("Name is required", invalidName.path("message").asText());
        request("POST", "/api/auth/register", registerBody().replace("\"User\"", "\"" + "x".repeat(101) + "\""), null, 400);
        JsonNode longPassword = request("POST", "/api/auth/register",
                registerBody().replace("Password123!", "\u00e9".repeat(37)), null, 400);
        assertEquals("Password must be at most 72 UTF-8 bytes", longPassword.path("message").asText());
        request("POST", "/api/auth/login", "{\"email\":\"user@example.com\",\"password\":null}", null, 400);
        request("POST", "/api/auth/register", registerBody().replace("Password123!", "\u00e9".repeat(36)), null, 201);
        request("POST", "/api/auth/login",
                "{\"email\":\"user@example.com\",\"password\":\"" + "\u00e9".repeat(36) + "\"}", null, 200);
    }

    private String registerBody() {
        return "{\"name\":\"User\",\"email\":\"user@example.com\",\"password\":\"Password123!\"}";
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private JsonNode request(String method, String path, String body, String token, int expected) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) request.header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(expected, response.statusCode(), response.body());
        JsonNode json = mapper.readTree(response.body());
        assertEquals(expected < 400, json.path("success").asBoolean(), response.body());
        assertTrue(json.has("message"));
        assertTrue(json.has("data"));
        return json;
    }
}
