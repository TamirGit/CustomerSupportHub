package com.example.supporthub;

import com.example.supporthub.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class CustomerSupportHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerSupportHubApplication.class, args);
    }
}
