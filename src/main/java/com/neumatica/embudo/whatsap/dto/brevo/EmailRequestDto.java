package com.neumatica.embudo.whatsap.dto.brevo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailRequestDto {

	private String to;
    private String name;
    private String company;
}
