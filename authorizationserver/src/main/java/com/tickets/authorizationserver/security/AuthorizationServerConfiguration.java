package com.tickets.authorizationserver.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.oauth2.server.authorization.OAuth2TokenType.ACCESS_TOKEN;

/**
 * AuthorizationServerConfiguration
 *
 * <p>
 * This configuration class wires up the Spring Authorization Server components used by the application.
 * It provides the SecurityFilterChain for the authorization endpoints, a JDBC-backed RegisteredClientRepository,
 * a token generator that delegates to a custom Jwt generator, a JWT customizer that adds authorities to the
 * access token, and a CORS configuration source used by the authorization endpoints.
 * </p>
 *
 * <p>Design notes (why this exists):
 * - Keep web security configuration for the authorization endpoints in one configuration class.
 * - Use a JDBC repository for RegisteredClient so client registrations persist in the database.
 * - Use a custom Jwt generator to control claims (subject uses UUID, include authorities, etc.).
 * </p>
 */
@Configuration
public class AuthorizationServerConfiguration {
    private final JwtConfiguration jwtConfiguration;

    public AuthorizationServerConfiguration(JwtConfiguration jwtConfiguration) {
        this.jwtConfiguration = jwtConfiguration;
    }

    /**
     * Security filter chain for the authorization server. Marked with @Order(1) so it takes priority
     * over the application's default web security filter chain.
     *
     * The method configures the Authorization Server endpoints using OAuth2AuthorizationServerConfigurer.
     * Note: several values are intentionally left as null/placeholders here — in a real deployment you
     * should provide concrete implementations (tokenGenerator, authenticationConverter, etc.).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerConfig(
            HttpSecurity http,
            RegisteredClientRepository registeredClientRepository,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator)
            throws Exception {

        var authorizationConfig =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.securityMatcher(authorizationConfig.getEndpointsMatcher())
                .with(authorizationConfig, authorizationServer ->
                        authorizationServer
                                .tokenGenerator(tokenGenerator)
                                .clientAuthentication(authentication -> {
                                    authentication.authenticationConverter(
                                            new ClientRefreshTokenAuthenticationConverter());

                                    authentication.authenticationProvider(
                                            new ClientAuthenticationProvider(
                                                    registeredClientRepository));
                                })
                                .oidc(Customizer.withDefaults())
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied")
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Enable CORS for authorization endpoints (source provided by corsConfigurationSource bean)
        http.cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()));
        // Configure authorization rules for the application endpoints.
        // The /login and /logout endpoints are publicly accessible,
        // while the /mfa endpoint requires the MFA_REQUIRED authority.
        // All other requests require authentication.
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login").permitAll()
                        .requestMatchers(POST, "/logout").permitAll()
                        .requestMatchers("/mfa").hasAuthority("MFA_REQUIRED")
                        .anyRequest().authenticated());
        // Configure form login with a custom login page and failure handler.
        http.formLogin(login -> login
                .loginPage("/login")
                // The success handler redirects to the /mfa endpoint and adds the MFA_REQUIRED authority to the authentication object.
                .successHandler(new MfaAuthenticationHandler("/mfa", "MFA_REQUIRED") {})
                .failureHandler(new SimpleUrlAuthenticationFailureHandler("/login?error"))
                .permitAll());
        // Configure logout to redirect to the specified URL and clear the JSESSIONID cookie upon logout.
        http.logout(logout -> logout
                .logoutSuccessUrl("http://localhost:3000")
                .addLogoutHandler(new CookieClearingLogoutHandler("JSESSIONID")));
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SavedRequestAwareAuthenticationSuccessHandler();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                //.issuer("http://localhost:8080")
                .build();
    }

    /**
     * RegisteredClientRepository bean backed by JDBC.
     *
     * This repository persists OAuth2 client registrations (clientId, clientSecret, redirectUris, scopes, etc.)
     * to the configured relational database using JdbcTemplate.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    /**
     * Token generator bean. Delegates to a custom Jwt generator (UserJwtGenerator) and a refresh token generator.
     * The custom Jwt generator is initialized here and the jwt customizer (which adds authorities claim) is wired in.
     */
    @Bean
    // The OAuth2TokenGenerator is responsible for generating access tokens and refresh tokens.
    // In this case, it delegates to a custom Jwt generator (UserJwtGenerator) and a refresh token generator (ClientOAuth2RefreshTokenGenerator).
    // IMPORTANT
    public OAuth2TokenGenerator<? extends OAuth2Token> oAuth2TokenGenerator() {
        var jwtGenerator = UserJwtGenerator.init(new NimbusJwtEncoder(jwtConfiguration.jwkSource()));
        jwtGenerator.setJwtCustomizer(customizer());
        OAuth2TokenGenerator<OAuth2RefreshToken> refreshTokenGenerator = new ClientOAuth2RefreshTokenGenerator();

        return new DelegatingOAuth2TokenGenerator(jwtGenerator, refreshTokenGenerator);
    }

    /**
     * Jwt customizer that adds the user's authorities into the access token claims under the key "authorities".
     * This runs when encoding access tokens and makes the authorities available as a claim in the resulting JWT.
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> customizer() {
        // Only add authorities to access tokens
        return context -> {
            if (ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getClaims().claim("authorities", getAuthorities(context));
            }
        };
    }

    /**
     * Helper that converts the Principal's GrantedAuthority list into a comma-separated string.
     * Example output: "user:read,user:write,ticket:read"
     */
    private String getAuthorities(JwtEncodingContext context) {
        return context.getPrincipal().getAuthorities().stream()
                // GrantedAuthority.getAuthority returns a String
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }

    /**
     * CORS configuration source used by the authorization endpoints. In development this allows any origin.
     * Production should restrict allowed origins, headers and methods appropriately.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOrigins(List.of("*"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
        corsConfiguration.setExposedHeaders(Arrays.asList("*"));
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST"));
        corsConfiguration.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return source;
    }

}
