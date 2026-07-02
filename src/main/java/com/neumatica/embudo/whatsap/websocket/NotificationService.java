package com.neumatica.embudo.whatsap.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.entitys.Contact;

@Service
public class NotificationService {

	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
	public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendNewContact(Contact contact){

        messagingTemplate.convertAndSend(
                "/topic/contacts",
                contact
        );

    }
}
