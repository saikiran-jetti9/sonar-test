package com.bmg.deliver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${cors.allowedOrigins}")
	String allowedOrigins;

	@Value("${cors.allowCredentials}")
	boolean allowCredentials;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedOrigins(allowedOrigins).allowCredentials(allowCredentials)
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
	}
}
