package com.goweyy.convoyia.pricer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.goweyy.convoyia.pricer", "com.goweyy.convoyia.common"})
public class PricerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PricerApplication.class, args);
    }
}
