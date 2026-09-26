package com.neumatica.embudo.whatsap.dto.webhook;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusDto {

    private String id;

    private String status;

    private String timestamp;

    @JsonProperty("recipient_id")
    private String recipientId;

    @JsonProperty("recipient_user_id")
    private String recipientUserId;

    private List<WhatsappErrorDto> errors = new ArrayList<>();
}