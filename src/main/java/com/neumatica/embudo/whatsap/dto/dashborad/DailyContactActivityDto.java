package com.neumatica.embudo.whatsap.dto.dashborad;

import java.time.LocalDate;

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
public class DailyContactActivityDto {

    private LocalDate date;

    private long newContacts;

}
