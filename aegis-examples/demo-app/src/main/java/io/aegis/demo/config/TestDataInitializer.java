package io.aegis.demo.config;

import io.aegis.apikey.store.ApiKeyStore;
import io.aegis.basic.credentials.CredentialsStore;
import io.aegis.core.user.DefaultUserPrincipal;
import io.aegis.core.user.UserPrincipal;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Initializes test data for the demo application.
 * Creates sample users, API keys, and credentials.
 */
@Component
public class TestDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TestDataInitializer.class);

    @Autowired(required = false)
    private ApiKeyStore apiKeyStore;

    @PostConstruct
    public void initializeTestData() {
        logger.info("Initializing test data for Aegis Demo Application");

        // Initialize API Keys
        if (apiKeyStore != null) {
            initializeApiKeys();
        }

        logger.info("Test data initialization complete");
    }

    private void initializeApiKeys() {
        logger.info("Creating test API keys");

        // API Key 1: Regular User
        UserPrincipal user1 = DefaultUserPrincipal.builder()
                .id("api-user-1")
                .username("alice")
                .email("alice@example.com")
                .displayName("Alice Johnson")
                .addRole("USER")
                .addPermission("read:data")
                .authenticationMechanism("API_KEY")
                .build();

        apiKeyStore.storeApiKey("demo-key-alice-12345", user1);
        logger.info("Created API key: demo-key-alice-12345 for user: alice");

        // API Key 2: Admin User
        UserPrincipal admin = DefaultUserPrincipal.builder()
                .id("api-user-2")
                .username("admin")
                .email("admin@example.com")
                .displayName("Admin User")
                .addRole("USER")
                .addRole("ADMIN")
                .addPermission("read:data")
                .addPermission("write:data")
                .addPermission("delete:data")
                .authenticationMechanism("API_KEY")
                .build();

        apiKeyStore.storeApiKey("demo-key-admin-67890", admin);
        logger.info("Created API key: demo-key-admin-67890 for user: admin");

        // API Key 3: Service Account
        UserPrincipal service = DefaultUserPrincipal.builder()
                .id("api-user-3")
                .username("service-bot")
                .email("service@example.com")
                .displayName("Service Bot")
                .addRole("SERVICE")
                .addPermission("read:data")
                .authenticationMechanism("API_KEY")
                .build();

        apiKeyStore.storeApiKey("demo-key-service-abc123", service);
        logger.info("Created API key: demo-key-service-abc123 for user: service-bot");
    }
}
