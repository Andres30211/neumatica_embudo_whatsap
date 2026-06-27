package com.neumatica.embudo.whatsap.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AudioDto {

	private String id;

    @JsonProperty("mime_type")
    private String mimeType;

    private String sha256;
}
