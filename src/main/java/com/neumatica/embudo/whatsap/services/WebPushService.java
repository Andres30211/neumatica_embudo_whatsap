package com.neumatica.embudo.whatsap.services;

import java.security.Security;

import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.entitys.PushSubscription;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebPushService {
	
	static {
	    Security.addProvider(new BouncyCastleProvider());
	}

    @Value("${webpush.public-key}")
    private String publicKey;

    @Value("${webpush.private-key}")
    private String privateKey;

    @Value("${webpush.subject}")
    private String subject;

    public void sendNotification(PushSubscription subscription, String message) throws Exception {

        PushService pushService = new PushService(
                publicKey,
                privateKey,
                subject
        );

        Notification notification = new Notification(
                subscription.getEndpoint(),
                subscription.getP256dh(),
                subscription.getAuth(),
                message
        );

        var response = pushService.send(notification);

        System.out.println("STATUS PUSH: "
                + response.getStatusLine().getStatusCode());

        System.out.println("BODY PUSH: "
                + EntityUtils.toString(response.getEntity()));
    }
}

