package com.neumatica.embudo.whatsap.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MessageDto {

	private String from;

    @JsonProperty("from_user_id")
    private String fromUserId;

    private String id;

    private String timestamp;

    private String type;

    private TextDto text;
}
