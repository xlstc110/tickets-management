package com.tickets.authorizationserver.security;

import com.tickets.authorizationserver.utils.UserUtils;
import org.springframework.lang.Nullable;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

/**
 * UserJwtGenerator
 *
 * <p>
 * Custom JWT generator used by the Authorization Server. This class implements
 * OAuth2TokenGenerator<Jwt> and produces a signed JWT for both access tokens
 * and ID tokens. It is based on Spring Authorization Server's JwtGenerator
 * implementation but adapted to the application's needs (for example, using
 * a stable user UUID as the subject claim instead of a mutable username).
 * </p>
 *
 * <p>Responsibilities:
 * - Build the JwtClaimsSet (iss, sub, aud, exp, iat, jti, scope, custom claims)
 * - Choose the signing algorithm (defaults to RS256, but respects the RegisteredClient settings for ID tokens)
 * - Optionally invoke an OAuth2TokenCustomizer to allow extra claims (authorities, etc.)
 * - Encode the claims and header into a signed Jwt using the provided JwtEncoder
 * </p>
 */
public class UserJwtGenerator implements OAuth2TokenGenerator<Jwt> {
    // Internal encoder used to sign/encode the Jwt
    private final JwtEncoder jwtEncoder;

    // Optional customizer that can add headers/claims before encoding
    private OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer;

    /**
     * Constructs a {@code UserJwtGenerator} with the provided JwtEncoder.
     * The encoder is required and must not be null.
     */
    public UserJwtGenerator(JwtEncoder jwtEncoder) {
        Assert.notNull(jwtEncoder, "jwtEncoder cannot be null");
        this.jwtEncoder = jwtEncoder;
    }

    /**
     * Generate a Jwt for the given OAuth2TokenContext.
     *
     * Steps:
     * 1. Validate the token type (only ACCESS_TOKEN and ID_TOKEN are supported here)
     * 2. Determine issuer, issuedAt and expiresAt
     * 3. Build the JwtClaimsSet, rewriting subject to use a stable user UUID
     * 4. Optionally customize the JwtEncodingContext via jwtCustomizer
     * 5. Encode the final JWS header + claims into a Jwt and return it
     *
     * Returns null when the context doesn't match expected conditions so that
     * other token generators in a DelegatingOAuth2TokenGenerator may handle it.
     */
    @Nullable
    @Override
    public Jwt generate(OAuth2TokenContext context) {
        // Only generate for supported token types
        if (context.getTokenType() == null ||
                (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType()) &&
                        !OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue()))) {
            return null;
        }

        // If access token format is not self-contained (JWT), skip generation here
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType()) &&
                !OAuth2TokenFormat.SELF_CONTAINED.equals(context.getRegisteredClient().getTokenSettings().getAccessTokenFormat())) {
            return null;
        }

        // Resolve issuer from authorization server context when available
        String issuer = null;
        if (context.getAuthorizationServerContext() != null) {
            issuer = context.getAuthorizationServerContext().getIssuer();
        }

        RegisteredClient registeredClient = context.getRegisteredClient();
        Instant issuedAt = Instant.now();
        Instant expiresAt;

        // Default signing algorithm (ID tokens may override this based on client settings)
        JwsAlgorithm jwsAlgorithm = SignatureAlgorithm.RS256;
        if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
            // ID Token TTL (example 30 minutes)
            expiresAt = issuedAt.plus(30, ChronoUnit.MINUTES);
            if (registeredClient.getTokenSettings().getIdTokenSignatureAlgorithm() != null) {
                jwsAlgorithm = registeredClient.getTokenSettings().getIdTokenSignatureAlgorithm();
            }
        } else {
            // Access token TTL comes from the client's token settings
            expiresAt = issuedAt.plus(registeredClient.getTokenSettings().getAccessTokenTimeToLive());
        }

        // Build the standard claims
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();
        if (StringUtils.hasText(issuer)) {
            claimsBuilder.issuer(issuer);
        }

        // Use a stable userUuid as the subject claim (prevents issues if username changes)
        claimsBuilder
                .subject(UserUtils.getUser(context.getPrincipal()).getUserUuid())
                .audience(Collections.singletonList(registeredClient.getClientId()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString());

        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            claimsBuilder.notBefore(issuedAt);
            if (!CollectionUtils.isEmpty(context.getAuthorizedScopes())) {
                claimsBuilder.claim(OAuth2ParameterNames.SCOPE, context.getAuthorizedScopes());
            }
        } else if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
            // ID Token specific claims
            claimsBuilder.claim(IdTokenClaimNames.AZP, registeredClient.getClientId());

            if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getAuthorizationGrantType())) {
                OAuth2AuthorizationRequest authorizationRequest = context.getAuthorization().getAttribute(
                        OAuth2AuthorizationRequest.class.getName());
                String nonce = (String) authorizationRequest.getAdditionalParameters().get(OidcParameterNames.NONCE);
                if (StringUtils.hasText(nonce)) {
                    claimsBuilder.claim(IdTokenClaimNames.NONCE, nonce);
                }

                SessionInformation sessionInformation = context.get(SessionInformation.class);
                if (sessionInformation != null) {
                    claimsBuilder.claim("sid", sessionInformation.getSessionId());
                    claimsBuilder.claim(IdTokenClaimNames.AUTH_TIME, sessionInformation.getLastRequest());
                }
            } else if (AuthorizationGrantType.REFRESH_TOKEN.equals(context.getAuthorizationGrantType())) {
                // Preserve sid and auth_time when rotating/refreshing ID tokens
                OidcIdToken currentIdToken = context.getAuthorization().getToken(OidcIdToken.class).getToken();
                if (currentIdToken.hasClaim("sid")) {
                    claimsBuilder.claim("sid", currentIdToken.getClaim("sid"));
                }
                if (currentIdToken.hasClaim(IdTokenClaimNames.AUTH_TIME)) {
                    claimsBuilder.claim(IdTokenClaimNames.AUTH_TIME, currentIdToken.<Date>getClaim(IdTokenClaimNames.AUTH_TIME));
                }
            }
        }

        // Build JWS header
        JwsHeader.Builder jwsHeaderBuilder = JwsHeader.with(jwsAlgorithm);

        // If a customizer is provided, build a JwtEncodingContext and allow it to modify headers/claims
        if (this.jwtCustomizer != null) {
            JwtEncodingContext.Builder jwtContextBuilder = JwtEncodingContext.with(jwsHeaderBuilder, claimsBuilder)
                    .registeredClient(context.getRegisteredClient())
                    .principal(context.getPrincipal())
                    .authorizationServerContext(context.getAuthorizationServerContext())
                    .authorizedScopes(context.getAuthorizedScopes())
                    .tokenType(context.getTokenType())
                    .authorizationGrantType(context.getAuthorizationGrantType());

            if (context.getAuthorization() != null) {
                jwtContextBuilder.authorization(context.getAuthorization());
            }
            if (context.getAuthorizationGrant() != null) {
                jwtContextBuilder.authorizationGrant(context.getAuthorizationGrant());
            }
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                SessionInformation sessionInformation = context.get(SessionInformation.class);
                if (sessionInformation != null) {
                    jwtContextBuilder.put(SessionInformation.class, sessionInformation);
                }
            }
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                Jwt dPoPProofJwt = context.get(OAuth2TokenContext.DPOP_PROOF_KEY);
                if (dPoPProofJwt != null) {
                    jwtContextBuilder.put(OAuth2TokenContext.DPOP_PROOF_KEY, dPoPProofJwt);
                }
            }

            JwtEncodingContext jwtContext = jwtContextBuilder.build();
            this.jwtCustomizer.customize(jwtContext);
        }

        // Encode header + claims into a signed Jwt
        JwsHeader jwsHeader = jwsHeaderBuilder.build();
        JwtClaimsSet claims = claimsBuilder.build();
        Jwt jwt = this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));

        return jwt;
    }

    /**
     * Sets the {@link OAuth2TokenCustomizer} that customizes the
     * {@link JwtEncodingContext#getJwsHeader() JWS headers} and/or
     * {@link JwtEncodingContext#getClaims() claims} for the generated {@link Jwt}.
     * @param jwtCustomizer the {@link OAuth2TokenCustomizer} that customizes the headers
     * and/or claims for the generated {@code Jwt}
     */
    public void setJwtCustomizer(OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
        Assert.notNull(jwtCustomizer, "jwtCustomizer cannot be null");
        this.jwtCustomizer = jwtCustomizer;
    }

    /**
     * Factory helper to create a UserJwtGenerator with a NimbusJwtEncoder.
     * Using a static init method keeps construction centralized and explicit.
     */
    public static UserJwtGenerator init(NimbusJwtEncoder jwtEncoder) {
        return new UserJwtGenerator(jwtEncoder);
    }
}
