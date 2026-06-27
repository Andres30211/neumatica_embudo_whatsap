package com.neumatica.embudo.whatsap.dto.webhook;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsappWebHookDto {

	private String object;
	
	private List<EntryDto> entry = new ArrayList<>();
}
