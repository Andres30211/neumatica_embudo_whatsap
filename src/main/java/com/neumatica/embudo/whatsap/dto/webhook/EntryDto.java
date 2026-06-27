package com.neumatica.embudo.whatsap.dto.webhook;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class EntryDto {

	private List<ChageDto> changes = new ArrayList<>();
}
