package com.neumatica.embudo.whatsap.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsappErrorDto {

    private Integer code;

    private String title;

    private String message;

    @JsonProperty("error_data")
    private ErrorDataDto errorData;
}
