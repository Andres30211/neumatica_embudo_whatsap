package com.neumatica.embudo.whatsap.services;
import java.util.List;

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
import com.neumatica.embudo.whatsap.dto.brevo.SenderDto;

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
	
	@Autowired
	private String brevoSenderName;
	
	@Autowired
	private String brevoSenderEmail;
	
	public void sendEmail(EmailRequestDto emailRequest) {

		BrevoEmailRequest body = new BrevoEmailRequest();

        body.setSenderDto(new SenderDto(this.brevoSenderName, this.brevoSenderEmail));

        body.setTo(List.of(
                new RecipientDto(
                		emailRequest.getTo(),
                		emailRequest.getName()
                )
        ));

        body.setSubject(emailRequest.getSubject());

        body.setHtmlContent(emailRequest.getHtml());
        
        body.setTemplateId(3L);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        headers.set("api-key", this.brevoSenderApiKey);

        HttpEntity<BrevoEmailRequest> entity =
                new HttpEntity<>(body, headers);

        try {
        	
        	System.out.println("Sender Name: " + this.brevoSenderName);
        	System.out.println("Sender Email: " + this.brevoSenderEmail);

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

            System.out.println("Código HTTP: " + e.getStatusCode());

            System.out.println("Respuesta Brevo:");

            System.out.println(e.getResponseBodyAsString());

            throw e;

        }

    }

    
}
