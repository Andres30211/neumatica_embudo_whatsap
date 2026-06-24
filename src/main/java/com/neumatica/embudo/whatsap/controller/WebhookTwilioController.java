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

        System.out.println("Usuario: " + profileName);
        System.out.println("Número: " + from);
        System.out.println("Mensaje: " + body);

        return ResponseEntity.ok("");
    }
}
