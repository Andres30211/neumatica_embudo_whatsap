package com.neumatica.embudo.whatsap.interfaces;

import java.time.LocalDate;

import com.neumatica.embudo.whatsap.enums.Direction;

public interface DailyMessageActivityProjection {

    LocalDate getDate();

    Direction getDirection();

    Long getTotal();
}
