package com.neumatica.embudo.whatsap.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.neumatica.embudo.whatsap.repository.WhatsappResponseAutimatics;

@Service
public class WhatsappResponseAutimaticsImpl implements WhatsappResponseAutimatics {

    private final RestClient restClient = RestClient.create();

    @Override
    public void sendText(String to, String message) {

        String url = "https://graph.facebook.com/v25.0/"
                + "1240032182526582"
                + "/messages";

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of(
                        "body", message
                )
        );

        System.out.println("\n==========================================");
        System.out.println("📤 ENVIANDO MENSAJE A WHATSAPP");
        System.out.println("==========================================");
        System.out.println("TO: " + to);
        System.out.println("MESSAGE: " + message);
        System.out.println("URL: " + url);

        try {

            ResponseEntity<String> response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + "EAAWNon6bi60BSpffbnHCRAEWZBw3ykoTowUCmyErZAAzp4NvY46IlH2yuySUVqPw2XZCabdvFJKs5c4X7oSXptCZAugouPvEMAU8ncxOFL8RaBSZBPQ2acYiLA8mTuZBSyjXWO19HSWDnVPKGq9rQra6CjJRY0ykEHjDbDNfgucdgumdZAHEAX44KtNwrH1UgZDZD")
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            System.out.println("\n✅ META ACEPTÓ EL MENSAJE");
            System.out.println("HTTP STATUS: " + response.getStatusCode());
            System.out.println("RESPONSE:");
            System.out.println(response.getBody());

        } catch (RestClientResponseException e) {

            System.err.println("\n❌ META RECHAZÓ EL MENSAJE");
            System.err.println("HTTP STATUS: " + e.getStatusCode());
            System.err.println("RESPONSE:");
            System.err.println(e.getResponseBodyAsString());

            throw e;

        } catch (Exception e) {

            System.err.println("\n❌ ERROR ENVIANDO MENSAJE");
            System.err.println("TIPO: " + e.getClass().getName());
            System.err.println("MENSAJE: " + e.getMessage());

            e.printStackTrace();

            throw e;
        }
    }
}