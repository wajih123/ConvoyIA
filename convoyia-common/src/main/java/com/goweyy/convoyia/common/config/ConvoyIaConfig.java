package com.goweyy.convoyia.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConvoyIaProperties.class)
public class ConvoyIaConfig {
}
