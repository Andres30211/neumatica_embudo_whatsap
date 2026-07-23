package com.neumatica.embudo.whatsap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WhatsapApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhatsapApplication.class, args);
	}

}
