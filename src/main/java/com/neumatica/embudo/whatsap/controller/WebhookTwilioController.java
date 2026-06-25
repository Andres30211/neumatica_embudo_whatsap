package com.neumatica.embudo.whatsap.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhookTwilioController {
	
	private static final Logger log = LoggerFactory.getLogger(WebhookTwilioController.class);

	@PostMapping
    public ResponseEntity<String> recibir(
            @RequestParam("From") String from,
            @RequestParam("Body") String body,
            @RequestParam("ProfileName") String profileName) {

		log.info("\nUsuario: ".concat(profileName + "\n")
        		.concat("")
        		.concat("Número: ".concat(from + "\n"))
        		.concat("")
        		.concat("Mensaje: ".concat(body)));
		
        return ResponseEntity.ok("Usuario: ".concat(profileName + "\n")
        		.concat("")
        		.concat("Número: ".concat(from + "\n"))
        		.concat("")
        		.concat("Mensaje: ".concat(body)));
    }
}
