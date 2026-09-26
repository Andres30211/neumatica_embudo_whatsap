package com.neumatica.embudo.whatsap.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;

import com.neumatica.embudo.whatsap.dto.contact.ConversationSummaryResponse;
import com.neumatica.embudo.whatsap.dto.webhook.WhatsappWebHookDto;
import com.neumatica.embudo.whatsap.entitys.Contact;

public interface WhatsappWebhookService {
	
	void delete(UUID id);
	
	Page<ConversationSummaryResponse> contacts(int page, String accessToken);

	void processWebhook(WhatsappWebHookDto webhook);
}
