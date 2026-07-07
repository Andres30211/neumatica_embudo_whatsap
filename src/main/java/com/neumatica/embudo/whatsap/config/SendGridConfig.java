package com.neumatica.embudo.whatsap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.sendgrid.SendGrid;

@Configuration
@PropertySource("classpath:sendgrid.properties")
public class SendGridConfig {

	@Value("${sendgrid.api.key}")
	private String apiKey;
	
	@Value("${sendgrid.email}")
	private String sender;
	
	@Bean
	public String returnSender() {
		return this.sender;
	}
	
	@Bean
	public SendGrid sendGrid() {
		return new SendGrid(apiKey);
	}
}
