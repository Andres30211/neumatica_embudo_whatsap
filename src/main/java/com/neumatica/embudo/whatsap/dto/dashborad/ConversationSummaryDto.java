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
public class ConversationSummaryDto {

    private long totalConversations;

    private long startedConversations;

    private long botConversations;

    private long humanConversations;

    private long closedConversations;
}
