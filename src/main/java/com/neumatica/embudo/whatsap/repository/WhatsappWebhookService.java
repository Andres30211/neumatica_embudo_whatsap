package com.neumatica.embudo.whatsap.repository;

import com.neumatica.embudo.whatsap.dto.webhook.WhatsappWebHookDto;

public interface WhatsappWebhookService {

	void processWebhook(WhatsappWebHookDto webhook);
}
