package com.neumatica.embudo.whatsap.interfaces;

import java.time.LocalDate;

public interface DailyContactActivityProjection {

    LocalDate getDate();

    Long getTotal();

}
