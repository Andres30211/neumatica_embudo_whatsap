package com.neumatica.embudo.whatsap.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhookTwilioController {
	
	@GetMapping
	public ResponseEntity<String> verify(
	        @RequestParam("hub.mode") String mode,
	        @RequestParam("hub.verify_token") String token,
	        @RequestParam("hub.challenge") String challenge) {

	    if ("subscribe".equals(mode)
	            && "andres123".equals(token)) {
	    	System.out.println(challenge);
	    	return ResponseEntity.ok(challenge);
	        
	    }

	    return ResponseEntity.status(403).build();
	}
	
	//private static final Logger log = LoggerFactory.getLogger(WebhookTwilioController.class);

	/*@PostMapping
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
    }*/
}
