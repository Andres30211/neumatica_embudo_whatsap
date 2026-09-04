package com.neumatica.embudo.whatsap.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neumatica.embudo.whatsap.entitys.Message;
import com.neumatica.embudo.whatsap.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook/messages")
@CrossOrigin(origins = {"http://localhost:4200", "https://neumatica-crm.netlify.app/"})
public class MessageMediaController {

    private final MessageRepository messageRepository;

    private final Path mediaRoot =
            Paths.get("./uploads/whatsapp")
                    .toAbsolutePath()
                    .normalize();

    @GetMapping("/{messageId}/media")
    public ResponseEntity<Resource> getMedia(
            @PathVariable String messageId) {

        Message message =
                messageRepository
                        .findByWhatsappMessageId(messageId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Mensaje no encontrado."
                                )
                        );

        if (message.getStoragePath() == null
                || message.getStoragePath().isBlank()) {

            return ResponseEntity.notFound().build();
        }

        String relativePath =
                message.getStoragePath()
                        .replaceFirst(
                                "^whatsapp/",
                                ""
                        );

        Path filePath =
                mediaRoot
                        .resolve(relativePath)
                        .normalize();

        /*
         * Seguridad:
         * evita que storagePath pueda salir
         * del directorio multimedia.
         */
        if (!filePath.startsWith(mediaRoot)) {
            return ResponseEntity.badRequest().build();
        }

        if (!Files.exists(filePath)
                || !Files.isRegularFile(filePath)) {

            return ResponseEntity.notFound().build();
        }

        try {

            Resource resource =
                    new UrlResource(
                            filePath.toUri()
                    );

            MediaType mediaType =
                    resolveMediaType(
                            message.getMimeType()
                    );

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline"
                    )
                    .body(resource);

        } catch (IOException exception) {

            throw new RuntimeException(
                    "No fue posible cargar el archivo multimedia.",
                    exception
            );
        }
    }

    private MediaType resolveMediaType(
            String mimeType) {

        if (mimeType == null
                || mimeType.isBlank()) {

            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {

            return MediaType.parseMediaType(
                    mimeType
            );

        } catch (Exception exception) {

            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
