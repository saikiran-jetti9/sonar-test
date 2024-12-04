package com.bmg.deliver;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@ImportAutoConfiguration({FeignAutoConfiguration.class})
@OpenAPIDefinition(info = @Info(title = "Delivery-Rebuild", version = "1.6.12"))
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
