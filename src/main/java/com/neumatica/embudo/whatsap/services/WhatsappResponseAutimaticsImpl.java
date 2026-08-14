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
	                + "1240032182526582"
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
	                .header("Authorization","Bearer " + "EAAWNon6bi60BSBJQsDz4UFjXGJGEq39Uuxo9fcgNj4QkDpm3WfrPdiZCUZBdGOzNU3u8A1tplKSXfGTlS8KC6NmARGiljZCKnQjTGw8ffJi89KosBh77yAKxZAI0qhWrOkZB2QIpnqyovQe5gchBEI0dX5M7pduHdIrITgOrpUxgMZCwRBiXGaPh7nOA9SHQZDZD")
	                .body(body)
	                .retrieve()
	                .toBodilessEntity();
	}

}
