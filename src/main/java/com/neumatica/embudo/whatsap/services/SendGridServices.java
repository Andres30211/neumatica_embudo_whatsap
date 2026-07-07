package com.neumatica.embudo.whatsap.services;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.dto.sendgrid.EmailRequestDto;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SendGridServices {

	@Autowired
	private SendGrid sendGrid;
	
	private String sender = "felifit27@gmail.com";
	
	public void sendEmail(EmailRequestDto request) throws IOException {

        Email from = new Email(this.sender);

        Email to = new Email(request.getTo());

        Content content = new Content("text/plain", request.getMessage());

        Mail mail = new Mail(from, request.getSubject(), to, content);

        Request sgRequest = new Request();

        sgRequest.setMethod(Method.POST);

        sgRequest.setEndpoint("mail/send");

        sgRequest.setBody(mail.build());

        this.sendGrid.api(sgRequest);

    }
}
