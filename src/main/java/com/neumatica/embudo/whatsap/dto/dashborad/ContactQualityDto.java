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
public class ContactQualityDto {

    private MetricValueDto phone;

    private MetricValueDto name;

    private MetricValueDto email;

    private MetricValueDto company;
}
