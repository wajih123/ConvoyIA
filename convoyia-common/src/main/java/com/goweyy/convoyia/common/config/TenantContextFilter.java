package com.goweyy.convoyia.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class TenantContextFilter implements WebFilter {

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String TENANT_CONTEXT_KEY = "tenantId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String tenantId = exchange.getRequest().getHeaders().getFirst(TENANT_ID_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            log.debug("No X-Tenant-Id header found in request to {}", exchange.getRequest().getPath());
        }
        return chain.filter(exchange)
                .contextWrite(ctx -> tenantId != null
                        ? ctx.put(TENANT_CONTEXT_KEY, tenantId)
                        : ctx);
    }
}
