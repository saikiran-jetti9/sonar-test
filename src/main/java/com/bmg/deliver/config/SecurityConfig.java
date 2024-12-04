package com.bmg.deliver.config;

import com.bmg.deliver.config.okta.CustomOAuth2AuthenticationSuccessHandler;
import com.bmg.deliver.config.okta.JwtSessionFilter;
import com.bmg.deliver.config.okta.OktaRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private final CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler;
	private final JwtSessionFilter jwtSessionFilter;

	public SecurityConfig(CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler,
			JwtSessionFilter jwtSessionFilter) {
		this.customOAuth2AuthenticationSuccessHandler = customOAuth2AuthenticationSuccessHandler;
		this.jwtSessionFilter = jwtSessionFilter;

	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.addFilterBefore(jwtSessionFilter, UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers("/actuator/**")
						.permitAll().anyRequest().authenticated())
				.oauth2Login(oAuth2Login -> oAuth2Login.loginPage("/oauth2/authorization/okta")
						.successHandler(customOAuth2AuthenticationSuccessHandler))
				.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.ignoringRequestMatchers("/ws-endpoint/**", "/api/**"))
				.httpBasic(AbstractHttpConfigurer::disable)
				.exceptionHandling(
						exceptionHandling -> exceptionHandling.authenticationEntryPoint(authenticationEntryPoint()))
				.oauth2ResourceServer(
						oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

		return http.build();
	}

	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint() {
		return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new OktaRoleConverter());
		return jwtAuthenticationConverter;
	}
}
