package com.neumatica.embudo.whatsap.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorDataDto {

    private String details;
}
