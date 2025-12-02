package com.bill.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        // ✅ Allow both forms — Live Server can use either 127.0.0.1 or localhost
                        .allowedOrigins("http://127.0.0.1:5500", "http://localhost:5500")
                        // ✅ Allow all HTTP methods your frontend might call
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        // ✅ Allow all headers (important for Fetch POST with body)
                        .allowedHeaders("*")
                        // ⚠️ Allow credentials *only* if you’re using cookies/auth tokens
                        .allowCredentials(false)
                        // ✅ Cache preflight response for 1 hour
                        .maxAge(3600);
            }
        };
    }
}
