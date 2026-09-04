package com.neumatica.embudo.whatsap.entitys;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.neumatica.embudo.whatsap.enums.RegistrationStep;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contact")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Número de WhatsApp.
     *
     * Puede ser null si Meta/WhatsApp no proporciona
     * el número del contacto.
     *
     * Si está disponible, debe ser único.
     */
    @Column(nullable = true, unique = true)
    private String phone;

    /*
     * Identificador interno de Meta/WhatsApp.
     *
     * Puede ser null dependiendo del evento recibido.
     */
    @Column(nullable = true)
    private String metaUserId;

    /*
     * Nombre mostrado por WhatsApp.
     *
     * También puede ser null si no está disponible.
     */
    @Column(nullable = true)
    private String name;

    /*
     * Email del contacto.
     */
    @Column(nullable = true)
    private String email;

    /*
     * Empresa del contacto.
     */
    @Column(nullable = true)
    private String company;

    /*
     * Paso actual del proceso de registro.
     */
    @Enumerated(EnumType.STRING)
    private RegistrationStep registrationStep;

    /*
     * Conversaciones del contacto.
     */
    @OneToMany(
        mappedBy = "contact",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<Conversation> conversations = new ArrayList<>();

    /*
     * Primer contacto registrado.
     */
    private LocalDateTime firstContact;

    /*
     * Última interacción.
     */
    private LocalDateTime lastInteraction;

    /*
     * Fecha de creación.
     */
    private LocalDateTime createdAt;
}
