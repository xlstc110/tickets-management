package com.tickets.gateway.security;

import com.tickets.gateway.handler.GatewayAccessDeniedHandler;
import com.tickets.gateway.handler.GatewayAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static com.tickets.gateway.constants.Roles.APP_READ;

@Configuration
@EnableWebSecurity
public class ResourceServerConfig {
    @Value("${jwks.uri}")
    private String jwkSetUri;

    @Bean
    @Order
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(null))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/user/register/**",
                                "/user/verify/account/**",
                                "/user/resetpassword/**",
                                "/user/images/**",
                                "/authorization/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .accessDeniedHandler(new GatewayAccessDeniedHandler())
                        .authenticationEntryPoint(new GatewayAuthenticationEntryPoint())
                        .jwt(jwt -> jwt.jwkSetUri(jwkSetUri)
                                .jwtAuthenticationConverter(new JwtConverter())));

        return http.build();
    }

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
