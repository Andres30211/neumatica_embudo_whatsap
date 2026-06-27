package com.neumatica.embudo.whatsap.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class VideoDto {

	private String id;

    private String caption;

    @JsonProperty("mime_type")
    private String mimeType;

    private String sha256;
}
