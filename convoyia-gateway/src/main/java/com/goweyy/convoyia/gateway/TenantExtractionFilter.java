package com.goweyy.convoyia.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that extracts the tenant_id claim from JWT
 * and forwards it downstream as X-Tenant-Id header.
 */
@Component
public class TenantExtractionFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(token -> {
                    Jwt jwt = token.getToken();
                    String tenantId = jwt.getClaimAsString("tenant_id");
                    if (tenantId != null && !tenantId.isBlank()) {
                        ServerHttpRequest mutated = exchange.getRequest().mutate()
                                .header("X-Tenant-Id", tenantId)
                                .build();
                        return exchange.mutate().request(mutated).build();
                    }
                    return exchange;
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
