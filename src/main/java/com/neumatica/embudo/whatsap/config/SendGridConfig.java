package com.neumatica.embudo.whatsap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sendgrid.SendGrid;

@Configuration
public class SendGridConfig {

	private String apiKey = "46zK1DiwQtyW1zYHijjfnw";
	
	@Bean
	private SendGrid sendGrid() {
		return new SendGrid(apiKey);
	}
}
