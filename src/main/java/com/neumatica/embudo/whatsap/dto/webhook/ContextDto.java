package com.neumatica.embudo.whatsap.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ContextDto {

	@JsonProperty("from")
    private String from;

    @JsonProperty("id")
    private String messageId;
}
