package com.neumatica.embudo.whatsap.services;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.neumatica.embudo.whatsap.dto.brevo.BrevoEmailRequest;
import com.neumatica.embudo.whatsap.dto.brevo.EmailRequestDto;
import com.neumatica.embudo.whatsap.dto.brevo.RecipientDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrevoEmailServices {

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private String brevoSenderApiKey;
	
	@Autowired
	private String brevoApiUrl;
	
	public void sendEmail(EmailRequestDto emailRequest, Long idTemplate) {

		BrevoEmailRequest body = new BrevoEmailRequest();

        body.setTo(List.of(
                new RecipientDto(
                		emailRequest.getTo()
                )
        ));
        
        body.setTemplateId(idTemplate);
        
        Map<String, String> params = new HashMap<>();
        params.put("email", emailRequest.getTo());
        params.put("name", emailRequest.getName());
        params.put("company", emailRequest.getCompany());
        
        body.setParams(params);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        headers.set("api-key", this.brevoSenderApiKey);

        HttpEntity<BrevoEmailRequest> entity =
                new HttpEntity<>(body, headers);

        try {

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            this.brevoApiUrl,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );
            
            

            System.out.println("Status: " + response.getStatusCode());

            System.out.println("Respuesta:");

            System.out.println(response.getBody());

        } catch (HttpStatusCodeException e) {
        	System.out.println("Brevo URL: " + brevoApiUrl);
            System.out.println("Brevo API Key configurada: "
                    + (brevoSenderApiKey != null && !brevoSenderApiKey.isBlank()));

            if (brevoSenderApiKey != null) {
                System.out.println("Longitud API Key: " + brevoSenderApiKey.length());
            }

            System.out.println("Código HTTP: " + e.getStatusCode());

            System.out.println("Respuesta Brevo:");

            System.out.println(e.getResponseBodyAsString());

            throw e;

        }

    }

    
}
