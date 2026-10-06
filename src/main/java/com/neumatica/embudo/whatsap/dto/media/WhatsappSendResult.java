package com.neumatica.embudo.whatsap.dto.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappSendResult {

    private String messageId;

    private String mediaId;
}
