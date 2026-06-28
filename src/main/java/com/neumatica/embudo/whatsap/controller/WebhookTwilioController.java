package com.neumatica.embudo.whatsap.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neumatica.embudo.whatsap.dto.webhook.WhatsappWebHookDto;
import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.repository.WhatsappWebhookService;

@RestController
@RequestMapping("/webhook")
public class WebhookTwilioController {
	
	@Autowired
	private WhatsappWebhookService whatsappWebhookService;
	
	@GetMapping("/contacts")
	public ResponseEntity<List<Contact>> contacts(){
		return ResponseEntity.ok(this.whatsappWebhookService.contacts());
	}
	
	@PostMapping
    public ResponseEntity<String> webhook(
            @RequestBody WhatsappWebHookDto webhook) {
		
		//System.out.println(webhook);
		this.whatsappWebhookService.processWebhook(webhook);

        return ResponseEntity.ok().build();
    }
}
