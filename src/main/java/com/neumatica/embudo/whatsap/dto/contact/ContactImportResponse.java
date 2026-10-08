package com.neumatica.embudo.whatsap.dto.contact;

import java.util.ArrayList;
import java.util.List;

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
public class ContactImportResponse {

    private int totalRows;

    private int created;

    private int updated;

    private int skipped;

    private int errors;

    @Builder.Default
    private List<ContactImportError> errorDetails = new ArrayList<>();
}
