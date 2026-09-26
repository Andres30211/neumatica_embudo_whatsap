package com.neumatica.embudo.whatsap.dto.dashborad;

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
public class MessageSummaryDto {

    private long totalMessages;

    private long receivedMessages;

    private long sentMessages;

    private long manualSentMessages;

    private long uniqueContactsWhoWrote;
}
