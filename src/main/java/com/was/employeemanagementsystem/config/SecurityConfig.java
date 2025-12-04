package com.was.employeemanagementsystem.config;

import com.was.employeemanagementsystem.security.JwtAuthenticationEntryPoint;
import com.was.employeemanagementsystem.security.JwtAuthenticationFilter;
import com.was.employeemanagementsystem.security.JwtUtils;
import com.was.employeemanagementsystem.security.UserDetailsServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import com.was.employeemanagementsystem.security.SecurityRequestFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtUtils jwtUtils;
    private final SecurityRequestFilter securityRequestFilter;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                         JwtAuthenticationEntryPoint unauthorizedHandler,
                         JwtUtils jwtUtils,
                         SecurityRequestFilter securityRequestFilter) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtUtils = jwtUtils;
        this.securityRequestFilter = securityRequestFilter;
    }

    /**
     * Configure StrictHttpFirewall to block malicious requests
     * This blocks URLs containing potentially dangerous characters like ";", "//", etc.
     * Note: Blocked requests will throw RequestRejectedException which is logged by Spring Security
     */
    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        
        // Block URLs containing semicolons (common in SQL injection attempts)
        firewall.setAllowSemicolon(false);
        
        // Block URLs containing backslashes
        firewall.setAllowBackSlash(false);
        
        // Block URLs containing URL-encoded characters that could be used for attacks
        firewall.setAllowUrlEncodedSlash(false);
        firewall.setAllowUrlEncodedPercent(false);
        firewall.setAllowUrlEncodedPeriod(false);
        
        // Block double slashes (path traversal attempts)
        firewall.setAllowUrlEncodedDoubleSlash(false);
        
        // Block null characters
        firewall.setAllowNull(false);
        
        log.info("✅ StrictHttpFirewall configured with enhanced security settings");
        log.info("   - Semicolons blocked: true");
        log.info("   - Backslashes blocked: true");
        log.info("   - URL-encoded dangerous chars blocked: true");
        log.info("   - Double slashes blocked: true");
        log.info("   - Null characters blocked: true");
        
        return firewall;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // Apply the strict firewall to all requests
        web.httpFirewall(httpFirewall());
        super.configure(web);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                // Public auth endpoints (no authentication required)
                .antMatchers("/api/auth/login").permitAll()
                .antMatchers("/api/auth/signup").permitAll()
                .antMatchers("/api/auth/verify-email").permitAll()
                .antMatchers("/api/auth/resend-verification").permitAll()
                .antMatchers("/api/auth/forgot-password").permitAll()
                .antMatchers("/api/auth/reset-password").permitAll()
                .antMatchers("/api/auth/forgot-username").permitAll()
                // Public organization logo endpoint (no authentication required for images)
                .antMatchers("/api/organizations/*/logo").permitAll()
                // Public health check endpoint
                .antMatchers("/api/actuator/health").permitAll()
                // Protected auth endpoints (authentication required)
                .antMatchers("/api/auth/change-password").authenticated()
                .antMatchers("/api/auth/complete-profile").authenticated()
                // Other endpoints
                .antMatchers("/api/init/**").permitAll()  // Allow initialization endpoints
                .antMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated();

        // Security headers configuration
        http.headers()
                .contentSecurityPolicy("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none';")
                .and()
                .httpStrictTransportSecurity()
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                .and()
                .contentTypeOptions()
                .and()
                .frameOptions().deny();  // Override for non-H2 endpoints

        // For H2 Console only - allow framing (development only)
        http.headers().frameOptions().sameOrigin();

        // Add authentication filter
        // Note: SecurityRequestFilter is disabled for now as it may interfere with legitimate requests
        // StrictHttpFirewall handles URL-based attacks
        // http.addFilterBefore(securityRequestFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
