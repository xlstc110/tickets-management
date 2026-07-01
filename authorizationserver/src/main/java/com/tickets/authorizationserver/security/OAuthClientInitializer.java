package com.tickets.authorizationserver.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;

import static java.util.UUID.randomUUID;

@Slf4j
@Component
public class OAuthClientInitializer implements ApplicationRunner {

    private final RegisteredClientRepository registeredClientRepository;

    @Value("${ui.app.url}")
    private String redirectUri;

    public OAuthClientInitializer(RegisteredClientRepository registeredClientRepository) {
        // The RegisteredClientRepository is a Spring Security interface that provides methods for managing registered OAuth2 clients.
        // It allows you to save, retrieve, and delete registered clients from the underlying data store (e.g., database).
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // If the client does not exist, the new client will be created and saved to the database.
        // Otherwise, every time this application starts, this will create the client again.
        if (registeredClientRepository.findByClientId("client") == null) {
            try {
                // Create a new RegisteredClient with the specified properties and save it to the repository.
                var registeredClient = RegisteredClient.withId(randomUUID().toString())
                        .clientId("client")
                        .clientSecret("secret")
                        // clientAuthenticationMethod specifies how the client will authenticate with the authorization server.
                        // Here is set to NONE, since the resource owner (User) will authenticate.
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                        // authorizationGrantType specifies the type of authorization grant that the client can use to obtain an access token.
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .scopes(scopes -> {
                            // The scopes specify the permissions that the client is requesting from the resource owner.
                            scopes.add(OidcScopes.OPENID);
                            scopes.add(OidcScopes.PROFILE);
                            scopes.add(OidcScopes.EMAIL);
                        })
                        // Specify the URI to which the user will be redirected after logging in.
                        .redirectUri(redirectUri)
                        // Specify the URI to which the user will be redirected after logging out.
                        // 8080 is where the server is running, and / is the root path of the application.
                        .postLogoutRedirectUri("http://localhost:8080")
                        // clientSettings specifies the settings for the client, such as whether to require user consent for authorization.
                        .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                        // tokenSettings specifies the settings for the access and refresh tokens issued to the client.
                        .tokenSettings(TokenSettings.builder()
                                .accessTokenTimeToLive(Duration.ofHours(1))
                                .refreshTokenTimeToLive(Duration.ofDays(30))
                                .build())
                        .build();
                // Save the registered client to the repository, which will persist it in the database.
                registeredClientRepository.save(registeredClient);
                log.info("OAuth2 client 'client' initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize OAuth2 client", e);
            }
        } else {
            log.info("OAuth2 client 'client' already exists, skipping initialization");
        }
    }
}
