package com.neumatica.embudo.whatsap.repository;

import com.neumatica.embudo.whatsap.entitys.Contact;

public interface WhatsappResponseAutimatics {

	void sendText(String to, String message);
}
