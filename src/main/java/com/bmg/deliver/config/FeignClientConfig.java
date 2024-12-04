package com.bmg.deliver.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String TOKEN = "Bearer internal";

	@Bean
	public Logger.Level feignLoggerLevel() {
		return Logger.Level.FULL;
	}

	@Bean
	public RequestInterceptor requestTokenBearerInterceptor() {
		return requestTemplate -> requestTemplate.header(AUTHORIZATION_HEADER, TOKEN);
	}
}
