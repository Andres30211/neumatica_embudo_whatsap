package com.neumatica.embudo.whatsap.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class StatusDto {

	private String id;

    private String status;

    private String timestamp;

    @JsonProperty("recipient_id")
    private String recipientId;

    @JsonProperty("recipient_user_id")
    private String recipientUserId;
}
