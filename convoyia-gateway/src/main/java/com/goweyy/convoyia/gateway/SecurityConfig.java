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
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/convoy/*/internal/**").permitAll()
                        .pathMatchers("/api/v1/convoy/verify/internal").permitAll()
                        .pathMatchers("/actuator/health/**").permitAll()
                        .pathMatchers("/api/v1/convoy/broadcast/heartbeat").hasRole("DRIVER")
                        .pathMatchers("/api/v1/convoy/broadcast/accept").hasRole("DRIVER")
                        .pathMatchers("/api/v1/convoy/broadcast/**").authenticated()
                        .pathMatchers("/api/v1/convoy/dispatch/**").hasAnyRole("CLIENT", "B2B")
                        .pathMatchers("/api/v1/convoy/verify/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/convoy/price/**").hasAnyRole("CLIENT", "B2B")
                        .pathMatchers("/api/v1/convoy/inspect/**").hasRole("DRIVER")
                        .pathMatchers("/api/v1/convoy/track/**").authenticated()
                        .pathMatchers("/api/v1/convoy/bill/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/convoy/schedule/**").authenticated()
                        .pathMatchers("/api/v1/convoy/dispute/**").authenticated()
                        .pathMatchers("/api/v1/convoy/dashboard/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/convoy/simulation/**").hasRole("ADMIN")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }
}
