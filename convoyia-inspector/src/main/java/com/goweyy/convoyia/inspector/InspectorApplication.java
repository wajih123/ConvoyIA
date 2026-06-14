package com.goweyy.convoyia.inspector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.goweyy.convoyia.inspector", "com.goweyy.convoyia.common"})
public class InspectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(InspectorApplication.class, args);
    }
}
