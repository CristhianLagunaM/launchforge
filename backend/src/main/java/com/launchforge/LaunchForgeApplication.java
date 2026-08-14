package com.launchforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LaunchForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LaunchForgeApplication.class, args);
    }
}
