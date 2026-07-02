package com.tickets.authorizationserver.security;

import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Base64;

public class ClientOAuth2RefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2RefreshToken> {
    private final StringKeyGenerator refreshTokenGenerator;

    public ClientOAuth2RefreshTokenGenerator() {
        this.refreshTokenGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding());
    }

    @Override
    public OAuth2RefreshToken generate(OAuth2TokenContext context) {
        // Check if the token type is REFRESH_TOKEN.
        // If not, return null to indicate that this generator does not handle other token types.
        if(!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
            return null;
        } else {
            var issueAt = Instant.now();
            // Calculate the expiration time for the refresh token based on the access token's time to live.
            var expireAt = issueAt.plus(context.getRegisteredClient().getTokenSettings().getAccessTokenTimeToLive());
            return new OAuth2RefreshToken(refreshTokenGenerator.generateKey(), issueAt, expireAt);
        }
    }
}
