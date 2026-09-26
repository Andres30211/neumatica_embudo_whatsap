package com.neumatica.embudo.whatsap.repository;

import java.time.LocalDate;

import com.neumatica.embudo.whatsap.dto.dashborad.DashboardSummaryDto;

public interface DashboardServiceRepository {

    DashboardSummaryDto getSummary(
            LocalDate from,
            LocalDate to
    );
}
