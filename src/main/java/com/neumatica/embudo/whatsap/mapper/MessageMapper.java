package com.neumatica.embudo.whatsap.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.neumatica.embudo.whatsap.dto.webhook.MessageDto;
import com.neumatica.embudo.whatsap.entitys.Conversation;
import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.enums.Direction;
import com.neumatica.embudo.whatsap.enums.MessageType;

@Component
public class MessageMapper {

	public Message toEntity(MessageDto dto,
            Conversation conversation){

		Message message = Message.builder()
		/*.conversation(conversation)*/
		.whatsappMessageId(dto.getId())
		.direction(Direction.INCOMING)
		.type(MessageType.valueOf(dto.getType().toUpperCase()))
		.whatsappTimestamp(Long.parseLong(dto.getTimestamp()))
		.createdAt(LocalDateTime.now())
		.build();

		switch (message.getType()) {
		
			case TEXT -> {
		
				if(dto.getText()!=null){
				
				    message.setBody(dto.getText().getBody());
				
				}
			
			}
		
			case IMAGE -> {
			
				if(dto.getImage()!=null){
				
				    message.setMediaId(dto.getImage().getId());
				
				    message.setCaption(dto.getImage().getCaption());
				
				    message.setMimeType(dto.getImage().getMimeType());
				
				    message.setSha256(dto.getImage().getSha256());
				
				}
			
			}
		
			case VIDEO -> {
			
				if(dto.getVideo()!=null){
				
				    message.setMediaId(dto.getVideo().getId());
				
				    message.setCaption(dto.getVideo().getCaption());
				
				    message.setMimeType(dto.getVideo().getMimeType());
				
				    message.setSha256(dto.getVideo().getSha256());
				
				}
			
			}
		
			case AUDIO -> {
			
				if(dto.getAudio()!=null){
				
				    message.setMediaId(dto.getAudio().getId());
				
				    message.setMimeType(dto.getAudio().getMimeType());
				
				    message.setSha256(dto.getAudio().getSha256());
				
				}
			
			}
		
			case DOCUMENT -> {
			
				if(dto.getDocument()!=null){
				
				    message.setMediaId(dto.getDocument().getId());
				
				    message.setMimeType(dto.getDocument().getMimeType());
				
				    message.setCaption(dto.getDocument().getCaption());
				
				    message.setSha256(dto.getDocument().getSha256());
				
				}
			
			}
		
		}
		
		return message;
		
		}
}
