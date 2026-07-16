package com.neumatica.embudo.whatsap.dto.brevo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BrevoEmailRequest {

	@JsonProperty("sender")
	private SenderDto senderDto;
    private List<RecipientDto> to;
    private String subject;
    private String htmlContent;
}
