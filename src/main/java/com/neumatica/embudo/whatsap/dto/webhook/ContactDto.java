package com.neumatica.embudo.whatsap.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ContactDto {

	private ProfileDto profile;

    @JsonProperty("wa_id")
    private String waId;

    @JsonProperty("user_id")
    private String userId;
}
