package com.neumatica.embudo.whatsap.dto.webhook;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ValueDto {

	@JsonProperty("messaging_product")
	private String messagingProduct;
	
	private MetadataDto metadata;

    private List<ContactDto> contacts;

    private List<MessageDto> messages = new ArrayList<>();
    
    private List<StatusDto> statuses = new ArrayList<>();
	
}
