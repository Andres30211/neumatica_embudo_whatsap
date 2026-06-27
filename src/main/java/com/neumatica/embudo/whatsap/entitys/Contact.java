package com.neumatica.embudo.whatsap.entitys;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "contact")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Contact {

	@Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Número de WhatsApp (único)
     */
    @Column(nullable = false, unique = true)
    private String phone;

    /**
     * Identificador interno de Meta
     */
    private String metaUserId;

    /**
     * Nombre mostrado por WhatsApp
     */
    @Column(nullable = false)
    private String name;

    /**
     * Se completará posteriormente
     */
    private String email;

    /**
     * Ciudad
     */
    private String city;

    /**
     * País
     */
    private String country;

    /**
     * Primer contacto
     */
    private LocalDateTime firstContact;

    /**
     * Última interacción
     */
    private LocalDateTime lastInteraction;

    private LocalDateTime createdAt;
}
