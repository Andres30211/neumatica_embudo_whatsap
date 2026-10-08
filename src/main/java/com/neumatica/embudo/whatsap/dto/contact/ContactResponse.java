package com.neumatica.embudo.whatsap.dto.contact;

import java.time.LocalDateTime;
import java.util.UUID;

import com.neumatica.embudo.whatsap.enums.RegistrationStep;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {

    private UUID id;

    private String name;

    private String phone;

    private String email;

    private String company;

    private RegistrationStep registrationStep;

    private String registrationStatus;

    private LocalDateTime firstContact;

    private LocalDateTime lastInteraction;

    private LocalDateTime createdAt;
}
