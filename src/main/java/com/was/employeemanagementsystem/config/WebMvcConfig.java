package com.was.employeemanagementsystem.config;

import com.was.employeemanagementsystem.security.OrganizationUuidInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * Registers interceptors for organization UUID validation
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final OrganizationUuidInterceptor organizationUuidInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(organizationUuidInterceptor)
                .addPathPatterns("/api/**")  // Apply to all API endpoints
                .excludePathPatterns(
                    "/api/auth/**",          // Exclude auth endpoints
                    "/api/init/**",          // Exclude initialization endpoints
                    "/api/health",           // Exclude health check
                    "/error"                 // Exclude error endpoint
                );
    }
}

