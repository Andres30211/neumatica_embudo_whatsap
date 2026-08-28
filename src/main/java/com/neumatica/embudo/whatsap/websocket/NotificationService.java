package com.neumatica.embudo.whatsap.websocket;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.dto.notification.Notification;
import com.neumatica.embudo.whatsap.entitys.Contact;

@Service
public class NotificationService {

	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
	public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendNewContact(Contact contact){

        this.messagingTemplate.convertAndSend(
                "/topic/contacts",
                contact
        );

    }
    
    public void sendNotification(Contact contact) {
    	
    	Notification notification = new Notification(
    			
    			UUID.randomUUID(),
    	        "NEW_WHATSAPP_MESSAGE",
    	        "Nuevo mensaje de WhatsApp",
    	        contact.getName() + " ha enviado un mensaje",
    	        contact.getId(),
    	        LocalDateTime.now(ZoneId.of("America/Bogota")),
    	        false
    			
    			);

	    
        messagingTemplate.convertAndSend(
                "/topic/notifications",
                notification
        );
    }
}
