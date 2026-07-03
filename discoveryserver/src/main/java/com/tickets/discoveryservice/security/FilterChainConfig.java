package com.tickets.discoveryservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

import static com.tickets.discoveryservice.constants.Roles.APP_READ;

@Configuration
@EnableWebSecurity
public class FilterChainConfig {
    private final UserDetailsService userDetailsService;

    public FilterChainConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    @Order
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
                        .userDetailsService(userDetailsService)
                .exceptionHandling(exception -> exception.accessDeniedHandler(new DiscoveryAccessDeniedHandler()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/eureka/fonts/**",
                                "/eureka/css/**",
                                "/eureka/js/**",
                                "/eureka/images/**",
                                "/icon/**").permitAll()
                        .requestMatchers("/eureka/**").hasAnyAuthority(APP_READ)
                        .anyRequest().authenticated())
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(new DiscoveryAuthenticationEntryPoint()));

        return http.build();
    }

}
