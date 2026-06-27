package com.neumatica.embudo.whatsap.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MessageDto {

	private String from;

    @JsonProperty("from_user_id")
    private String fromUserId;

    private String id;

    private String timestamp;

    private String type;

    private TextDto text;
    
    private ImageDto image;

    private VideoDto video;

    private AudioDto audio;

    private DocumentDto document;

    private StickerDto sticker;

    private LocationDto location;

    private ContextDto context;
}
