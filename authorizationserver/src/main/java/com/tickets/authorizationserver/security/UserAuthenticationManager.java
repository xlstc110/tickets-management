package com.tickets.authorizationserver.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.stereotype.Component;

@Configuration
public class UserAuthenticationManager {
    private final UserAuthenticationProvider authenticationProvider;

    public UserAuthenticationManager(UserAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    // This bean is used to create an AuthenticationManager that uses the custom UserAuthenticationProvider for authentication.
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider);
    }
}
