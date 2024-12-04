package com.bmg.deliver.config;

import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

import java.util.List;

@Configuration
@EnableJms
@Slf4j
public class ActiveMqConfig {

	@Value("${spring.activemq.broker-url}")
	private String brokerUrl;

	@Value(value = "${spring.activemq.user}")
	private String user;

	@Value(value = "${spring.activemq.password}")
	private String password;

	@Bean
	public ConnectionFactory connectionFactory() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
		factory.setTrustedPackages(List.of("com.bmg.deliver"));
		factory.setBrokerURL(brokerUrl);
		factory.setUserName(user);
		factory.setPassword(password);
		factory.setExceptionListener(e -> log.error("JMS Exception occurred. {}", ExceptionUtils.getStackTrace(e)));
		return factory;
	}

	@Bean
	public JmsTemplate jmsTemplate() {
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory());
		jmsTemplate.setPubSubDomain(true);
		return jmsTemplate;
	}
}
