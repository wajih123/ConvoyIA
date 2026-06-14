package com.goweyy.convoyia.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                // CSRF disabled: this is a stateless REST API gateway that uses JWT ******
                // CSRF attacks require cookie-based authentication — ****** are not sent
                // automatically by browsers and are therefore not vulnerable to CSRF.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Internal endpoints skip auth
                        .pathMatchers("/api/v1/convoy/*/internal/**").permitAll()
                        // Actuator health checks
                        .pathMatchers("/actuator/health/**").permitAll()
                        // All other routes require JWT
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {})
                )
                .build();
    }
}
