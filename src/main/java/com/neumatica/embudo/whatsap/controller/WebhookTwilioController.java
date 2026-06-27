package com.neumatica.embudo.whatsap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neumatica.embudo.whatsap.dto.webhook.WhatsappWebHookDto;

@RestController
@RequestMapping("/webhook")
public class WebhookTwilioController {
	
	@PostMapping
    public ResponseEntity<String> webhook(
            @RequestBody WhatsappWebHookDto webhook) {

        System.out.println(webhook.getEntry().getFirst().getChanges().getFirst().getValue().getMessagingProduct());
        System.out.println(webhook.getEntry().getFirst().getChanges().getFirst().getValue().getMetadata().getDisplayPhoneNumber());
        System.out.println(webhook.getEntry().getFirst().getChanges().getFirst().getValue().getContacts().getFirst().getProfile().getName());
        System.out.println(webhook.getEntry().getFirst().getChanges().getFirst().getValue().getContacts().getFirst().getWaId());
        System.out.println(webhook.getEntry().getFirst().getChanges().getFirst().getValue().getMessages().getFirst().getText().getBody());

        return ResponseEntity.ok("Si llego el mensaje...");
    }
}
