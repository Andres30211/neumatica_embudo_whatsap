package com.neumatica.embudo.whatsap.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neumatica.embudo.whatsap.dto.push.PushSubscriptionRequest;
import com.neumatica.embudo.whatsap.entitys.PushSubscription;
import com.neumatica.embudo.whatsap.services.PushSubscriptionService;
import com.neumatica.embudo.whatsap.services.WebPushService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/push/subscriptions")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;
    
    private final WebPushService webPushService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PushSubscription registerSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PushSubscriptionRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());

        return pushSubscriptionService.registerSubscription(
                userId,
                request
        );
    }
    
    @GetMapping("/test")
    public String testPush(
            @AuthenticationPrincipal Jwt jwt
    ) throws Exception {

        UUID userId = UUID.fromString(jwt.getSubject());

        PushSubscription subscription =
                pushSubscriptionService.getFirstSubscription(userId);
        
        String payload = """
        		{
        		  "notification": {
        		    "title": "Prueba de notificación",
        		    "body": "¡Hola! Esta es tu primera notificación Push 🚀",
        		    "icon": "/assets/icons/icon-192x192.png"
        		  }
        		}
        		""";

        webPushService.sendNotification(subscription, payload);

        return "Notificación enviada correctamente";
    }
}

