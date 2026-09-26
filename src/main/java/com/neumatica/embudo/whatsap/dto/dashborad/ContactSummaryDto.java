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
public class ContactSummaryDto {

    private long totalContacts;

    private long newContacts;

    private long activeContacts;

    private double activePercentage;
}
