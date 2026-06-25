package com.neumatica.embudo.whatsap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhookTwilioController {

	@PostMapping
    public ResponseEntity<String> recibir(
            @RequestParam("From") String from,
            @RequestParam("Body") String body,
            @RequestParam("ProfileName") String profileName) {

        return ResponseEntity.ok("Usuario: ".concat(profileName + "\n")
        		.concat("")
        		.concat("Número: ".concat(from + "\n"))
        		.concat("")
        		.concat("Mensaje: ".concat(body)));
    }
}
