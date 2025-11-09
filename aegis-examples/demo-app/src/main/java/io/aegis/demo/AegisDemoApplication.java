package io.aegis.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aegis Authentication SDK Demo Application.
 *
 * This application demonstrates all authentication mechanisms supported by Aegis:
 * - JWT (JSON Web Tokens)
 * - OAuth 2.0 / OpenID Connect
 * - API Keys
 * - Basic Authentication
 * - Session-based Authentication
 *
 * Access the demo UI at: http://localhost:8080
 */
@SpringBootApplication
public class AegisDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AegisDemoApplication.class, args);
    }
}
