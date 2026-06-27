package com.neumatica.embudo.whatsap.dto.webhook;

import lombok.Data;

@Data
public class LocationDto {

	private Double latitude;

    private Double longitude;

    private String name;

    private String address;
}
