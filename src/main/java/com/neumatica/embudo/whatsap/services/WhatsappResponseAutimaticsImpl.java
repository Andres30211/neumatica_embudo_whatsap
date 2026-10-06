package com.neumatica.embudo.whatsap.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neumatica.embudo.whatsap.dto.media.WhatsappSendResult;
import com.neumatica.embudo.whatsap.repository.WhatsappResponseAutimatics;

@Service
public class WhatsappResponseAutimaticsImpl
        implements WhatsappResponseAutimatics {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String phoneNumberId;

    private final String accessToken;

    private final String graphApiVersion;

    public WhatsappResponseAutimaticsImpl(
            ObjectMapper objectMapper,

            @Value("${whatsapp.phone-number-id}")
            String phoneNumberId,

            @Value("${whatsapp.access-token}")
            String accessToken,

            @Value("${whatsapp.graph-api-version:v25.0}")
            String graphApiVersion
    ) {

        this.restClient =
                RestClient.create();

        this.objectMapper =
                objectMapper;

        this.phoneNumberId =
                phoneNumberId;

        this.accessToken =
                accessToken;

        this.graphApiVersion =
                graphApiVersion;
    }

    // ============================================================
    // ENVIAR TEXTO
    // ============================================================

    @Override
    public WhatsappSendResult sendText(
            String to,
            String message
    ) {

        String url =
                buildMessagesUrl();

        Map<String, Object> body =
                Map.of(
                        "messaging_product",
                        "whatsapp",

                        "to",
                        to,

                        "type",
                        "text",

                        "text",
                        Map.of(
                                "body",
                                message
                        )
                );

        try {

            ResponseEntity<String> response =
                    restClient.post()
                            .uri(url)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + accessToken
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(body)
                            .retrieve()
                            .toEntity(String.class);

            return parseSendResult(
                    response.getBody(),
                    null
            );

        } catch (RestClientResponseException e) {

            logMetaError(
                    "ERROR ENVIANDO TEXTO",
                    e
            );

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error enviando mensaje de texto a WhatsApp.",
                    e
            );
        }
    }

    // ============================================================
    // SUBIR MEDIA A WHATSAPP
    // ============================================================

    @Override
    public String uploadMedia(
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "El archivo es obligatorio."
            );
        }

        String url =
                "https://graph.facebook.com/"
                        + graphApiVersion
                        + "/"
                        + phoneNumberId
                        + "/media";

        try {

            byte[] bytes =
                    file.getBytes();

            ByteArrayResource resource =
                    new ByteArrayResource(bytes) {

                        @Override
                        public String getFilename() {

                            return file.getOriginalFilename();
                        }
                    };

            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            body.add(
                    "messaging_product",
                    "whatsapp"
            );

            body.add(
                    "file",
                    resource
            );

            body.add(
                    "type",
                    file.getContentType() != null
                            ? file.getContentType()
                            : "application/octet-stream"
            );

            ResponseEntity<String> response =
                    restClient.post()
                            .uri(url)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + accessToken
                            )
                            .contentType(
                                    MediaType.MULTIPART_FORM_DATA
                            )
                            .body(body)
                            .retrieve()
                            .toEntity(String.class);

            String responseBody =
                    response.getBody();

            JsonNode json =
                    objectMapper.readTree(
                            responseBody
                    );

            JsonNode idNode =
                    json.get("id");

            if (
                    idNode == null
                            || idNode.asText().isBlank()
            ) {

                throw new RuntimeException(
                        "WhatsApp no devolvió el mediaId."
                );
            }

            return idNode.asText();

        } catch (RestClientResponseException e) {

            logMetaError(
                    "ERROR SUBIENDO MEDIA",
                    e
            );

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error subiendo archivo a WhatsApp.",
                    e
            );
        }
    }

    // ============================================================
    // ENVIAR IMAGEN
    // ============================================================

    @Override
    public WhatsappSendResult sendImage(
            String to,
            String mediaId,
            String caption
    ) {

        Map<String, Object> image =
                new java.util.HashMap<>();

        image.put(
                "id",
                mediaId
        );

        if (
                caption != null
                        && !caption.isBlank()
        ) {

            image.put(
                    "caption",
                    caption
            );
        }

        Map<String, Object> body =
                Map.of(
                        "messaging_product",
                        "whatsapp",

                        "to",
                        to,

                        "type",
                        "image",

                        "image",
                        image
                );

        return sendMediaMessage(
                body,
                mediaId
        );
    }

    // ============================================================
    // ENVIAR DOCUMENTO
    // ============================================================

    @Override
    public WhatsappSendResult sendDocument(
            String to,
            String mediaId,
            String caption,
            String fileName
    ) {

        Map<String, Object> document =
                new java.util.HashMap<>();

        document.put(
                "id",
                mediaId
        );

        if (
                caption != null
                        && !caption.isBlank()
        ) {

            document.put(
                    "caption",
                    caption
            );
        }

        if (
                fileName != null
                        && !fileName.isBlank()
        ) {

            document.put(
                    "filename",
                    fileName
            );
        }

        Map<String, Object> body =
                Map.of(
                        "messaging_product",
                        "whatsapp",

                        "to",
                        to,

                        "type",
                        "document",

                        "document",
                        document
                );

        return sendMediaMessage(
                body,
                mediaId
        );
    }

    // ============================================================
    // MÉTODO INTERNO PARA ENVIAR MEDIA
    // ============================================================

    private WhatsappSendResult sendMediaMessage(
            Map<String, Object> body,
            String mediaId
    ) {

        String url =
                buildMessagesUrl();

        try {

            ResponseEntity<String> response =
                    restClient.post()
                            .uri(url)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + accessToken
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(body)
                            .retrieve()
                            .toEntity(String.class);

            return parseSendResult(
                    response.getBody(),
                    mediaId
            );

        } catch (RestClientResponseException e) {

            logMetaError(
                    "ERROR ENVIANDO MEDIA",
                    e
            );

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error enviando multimedia a WhatsApp.",
                    e
            );
        }
    }

    // ============================================================
    // CONSTRUIR URL DE MENSAJES
    // ============================================================

    private String buildMessagesUrl() {

        return "https://graph.facebook.com/"
                + graphApiVersion
                + "/"
                + phoneNumberId
                + "/messages";
    }

    // ============================================================
    // LEER RESPUESTA DE META
    // ============================================================

    private WhatsappSendResult parseSendResult(
            String responseBody,
            String mediaId
    ) {

        try {

            JsonNode json =
                    objectMapper.readTree(
                            responseBody
                    );

            JsonNode messages =
                    json.get("messages");

            String messageId = null;

            if (
                    messages != null
                            && messages.isArray()
                            && !messages.isEmpty()
            ) {

                JsonNode first =
                        messages.get(0);

                JsonNode id =
                        first.get("id");

                if (id != null) {

                    messageId =
                            id.asText();
                }
            }

            return WhatsappSendResult.builder()
                    .messageId(messageId)
                    .mediaId(mediaId)
                    .build();

        } catch (Exception e) {

            throw new RuntimeException(
                    "No fue posible interpretar la respuesta de WhatsApp.",
                    e
            );
        }
    }

    // ============================================================
    // LOG DE ERROR
    // ============================================================

    private void logMetaError(
            String title,
            RestClientResponseException e
    ) {

        System.err.println(
                "\n=========================================="
        );

        System.err.println(
                "❌ " + title
        );

        System.err.println(
                "=========================================="
        );

        System.err.println(
                "HTTP STATUS: "
                        + e.getStatusCode()
        );

        System.err.println(
                "RESPONSE:"
        );

        System.err.println(
                e.getResponseBodyAsString()
        );
    }
}