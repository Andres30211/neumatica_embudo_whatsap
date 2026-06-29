package com.neumatica.embudo.whatsap.services;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.neumatica.embudo.whatsap.repository.WhatsappResponseAutimatics;

@Service
public class WhatsappResponseAutimaticsImpl implements WhatsappResponseAutimatics{
	
	private final RestClient restClient = RestClient.create();

	@Override
	public void sendText(String to, String message) {
		
		 String url = "https://graph.facebook.com/v25.0/"
	                + "1228471667014746"
	                + "/messages";

	        Map<String,Object> body = Map.of(
	                "messaging_product","whatsapp",
	                "to",to,
	                "type","text",
	                "text",Map.of(
	                        "body",message
	                )
	        );

	        restClient.post()
	                .uri(url)
	                .header("Authorization","Bearer " + "EAAWNon6bi60BR38ZCZABKHfRHkJtIXyKv7chgZB7P3jIJ75iazoO4HqIe77eHx2iu1QUhE6ZABiqXxPiHe4XeJ12tdQpOyxJ566L8SOl2GB9pir4g9mvQUlK0UMS1AFg7q7iyA9oHjWgl86bTClHWWLuFAaDMcpnqks3ZClMDdkbFhNtPRruGHDjRVZAYOCwZDZD")
	                .body(body)
	                .retrieve()
	                .toBodilessEntity();
	}

}
