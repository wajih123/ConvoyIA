package com.goweyy.convoyia.biller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.goweyy.convoyia.biller", "com.goweyy.convoyia.common"})
public class BillerApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillerApplication.class, args);
    }
}
