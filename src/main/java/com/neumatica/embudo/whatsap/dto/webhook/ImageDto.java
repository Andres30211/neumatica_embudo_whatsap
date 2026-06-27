package com.neumatica.embudo.whatsap.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ImageDto {

	private String id;

    private String caption;

    @JsonProperty("mime_type")
    private String mimeType;

    private String sha256;

    /**
     * Normalmente este campo NO viene en el webhook.
     * Para obtener la URL debes consultar la Graph API usando el id.
     */
    private String url;
}
