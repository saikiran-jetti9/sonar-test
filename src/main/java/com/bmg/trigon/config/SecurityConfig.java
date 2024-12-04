package com.bmg.trigon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Configures the security filter chain for HTTP requests.
   *
   * @param http the HttpSecurity object to configure.
   * @return the configured SecurityFilterChain.
   * @throws Exception if an error occurs during configuration.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authz -> authz.anyRequest().authenticated() // All endpoints require authentication
            )
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(Customizer.withDefaults()) // Enable JWT validation
            );

    return http.build();
  }
}
