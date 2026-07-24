package com.neumatica.embudo.whatsap.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neumatica.embudo.whatsap.dto.webhook.MessageDto;
import com.neumatica.embudo.whatsap.dto.webhook.WhatsappWebHookDto;
import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.repository.WhatsappWebhookService;
import com.neumatica.embudo.whatsap.services.ExcelExportServices;
import com.neumatica.embudo.whatsap.services.WhatsappWebhookServiceImpl;

@RestController
@RequestMapping("/webhook")
@CrossOrigin(origins = {"http://localhost:4200", "https://neumatica-crm.netlify.app/"})
public class WebhookMetaController {
	
	@Autowired
	private WhatsappWebhookService whatsappWebhookService;
	
	@Autowired
	private WhatsappWebhookServiceImpl impl;
	
	@Autowired
	private ExcelExportServices excelExportServices;
	
	@DeleteMapping("/delete/{id}")
	public void delete(@PathVariable("id") UUID id){
		this.whatsappWebhookService.delete(id);
	}
	
	@GetMapping("/contacts")
	public ResponseEntity<List<Contact>> contacts(){
		return ResponseEntity.ok(this.whatsappWebhookService.contacts());
	}
	
	/*@PostMapping
    public void webhook(@RequestBody WhatsappWebHookDto json) {
		
		System.out.println(json);
    }*/
	
	@PostMapping
    public ResponseEntity<String> webhook(@RequestBody WhatsappWebHookDto webhook) {
		
		System.out.println(webhook);
		this.whatsappWebhookService.processWebhook(webhook);

        return ResponseEntity.ok().build();
    }
	
	@PostMapping("/sendCampaing")
	public ResponseEntity<String> sendCampaing(){
		
		this.impl.sendCampaing();
		
		return ResponseEntity.ok("Comienzó de envió de Campaña...");
	}
	
	@GetMapping("/export")
    public ResponseEntity<byte[]> exportarExcel() {

        byte[] excel = this.excelExportServices.exportContactsExcel();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=contactos.xlsx"
                )
                .contentType(
                        MediaType.APPLICATION_OCTET_STREAM
                )
                .body(excel);

    }
}
