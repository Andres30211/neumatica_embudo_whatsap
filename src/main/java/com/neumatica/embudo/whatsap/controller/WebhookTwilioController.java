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
	
	@GetMapping("/webhook")
	public ResponseEntity<String> verify(
	        @RequestParam("hub.mode") String mode,
	        @RequestParam("hub.verify_token") String token,
	        @RequestParam("hub.challenge") String challenge) {

	    if ("subscribe".equals(mode)
	            && "EAAWNon6bi60BR8JQOyIme5BIeBXpS2fZCW20ovBfXem8Y0hoOT7TZBdFCnNcxnlA5yZBcZBZADWXTFButXiVoysZBSiS91Rnx9pZCNCHdVCgbUDFsabZBu7cd4y5SFMSYtroBSdjPqZAziQChoKoSkseDqfs9DrLrM0WbA13tvk8bRm8ZBCeHXUHYzd5fD1N1dXHwGD7HQtsZCGr9REpIpQGZAWyr13atTVXNlsqsN5wRRAzFSvP0oZBZBW9oZB78axU99rxcQpARGivZBZAgcTlfwCAeKd0f".equals(token)) {
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
