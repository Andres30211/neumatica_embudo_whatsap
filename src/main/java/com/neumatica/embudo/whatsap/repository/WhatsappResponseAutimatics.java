package com.neumatica.embudo.whatsap.repository;

import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

import com.neumatica.embudo.whatsap.dto.media.WhatsappSendResult;

public interface WhatsappResponseAutimatics {

	WhatsappSendResult sendText(String to, String message);
	
	String uploadMedia(
            MultipartFile file
    );

    WhatsappSendResult sendImage(
            String to,
            String mediaId,
            String caption
    );

    WhatsappSendResult sendDocument(
            String to,
            String mediaId,
            String caption,
            String fileName
    );
    
    String uploadAudio(Path audioPath, String fileName, String mimeType);

    WhatsappSendResult sendAudio(String to, String mediaId);
}
