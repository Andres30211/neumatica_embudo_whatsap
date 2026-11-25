package com.neumatica.embudo.whatsap.dto.contact;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactBatchRequest(

    @NotEmpty(message = "Debe proporcionar al menos un contacto")
    @Size(max = 100, message = "No se pueden consultar más de 100 contactos")
    List<@NotNull UUID> ids

) {}
