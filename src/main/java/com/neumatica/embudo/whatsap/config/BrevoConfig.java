package com.neumatica.embudo.whatsap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;

@Configuration
@PropertySource("classpath:brevo.properties")
public class BrevoConfig {

	@Value("${brevo.api.key}")
	private String apiKey;
	
	@Value("${brevo.api.url}")
	private String apiUrl;
	
	@Bean
	public String brevoSenderApiKey() {
		return this.apiKey;
	}
	
	@Bean
	public String brevoApiUrl() {
		return this.apiUrl;
	}
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
