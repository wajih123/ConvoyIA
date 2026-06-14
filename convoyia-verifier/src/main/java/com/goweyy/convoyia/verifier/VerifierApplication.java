package com.goweyy.convoyia.verifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.goweyy.convoyia.verifier", "com.goweyy.convoyia.common"})
public class VerifierApplication {
    public static void main(String[] args) {
        SpringApplication.run(VerifierApplication.class, args);
    }
}
