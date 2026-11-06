package com.neumatica.embudo.whatsap.dto.push;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscriptionRequest {

    private String endpoint;

    private String p256dh;

    private String auth;
}

