package com.neumatica.embudo.whatsap.dto.dashborad;

import java.util.List;

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
public class DashboardSummaryDto {

    private ContactSummaryDto contacts;
    
    private MessageSummaryDto messages;
    
    private List<DailyMessageActivityDto> messageActivity;
    
    private ContactQualityDto contactQuality;
    
    private ContactQualityDto contactQualityDto;
    
    private ConversationSummaryDto conversations;
    
    private List<DailyContactActivityDto> contactActivity;
}
