package com.neumatica.embudo.whatsap.services;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.neumatica.embudo.whatsap.dto.contact.ContactImportError;
import com.neumatica.embudo.whatsap.dto.contact.ContactImportResponse;
import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.enums.RegistrationStep;
import com.neumatica.embudo.whatsap.repository.ContactImportService;
import com.neumatica.embudo.whatsap.repository.ContactRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactImportServiceImpl implements ContactImportService {

    private static final String EXCEL_EXTENSION = ".xlsx";

    private static final String PHONE_HEADER = "phone";
    private static final String NAME_HEADER = "name";
    private static final String EMAIL_HEADER = "email";
    private static final String COMPANY_HEADER = "company";

    private static final ZoneId COLOMBIA_ZONE =
            ZoneId.of("America/Bogota");

    private final ContactRepository contactRepository;

    private final DataFormatter dataFormatter =
            new DataFormatter();

    @Override
    @Transactional
    public ContactImportResponse importContacts(
            MultipartFile file) {

        validateFile(file);

        int totalRows = 0;
        int created = 0;
        int updated = 0;
        int skipped = 0;

        List<ContactImportError> errors =
                new ArrayList<>();

        /*
         * Controlamos teléfonos repetidos dentro
         * del mismo archivo Excel.
         */
        Set<String> phonesProcessed =
                new HashSet<>();

        try (
                InputStream inputStream =
                        file.getInputStream();

                Workbook workbook =
                        new XSSFWorkbook(inputStream)
        ) {

            Sheet sheet =
                    workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() <= 1) {

                throw new IllegalArgumentException(
                        "El archivo Excel no contiene contactos."
                );
            }

            Row headerRow =
                    sheet.getRow(0);

            validateHeaders(headerRow);

            for (int i = 1;
                 i <= sheet.getLastRowNum();
                 i++) {

                Row row =
                        sheet.getRow(i);

                /*
                 * Ignoramos filas completamente vacías.
                 */
                if (isEmptyRow(row)) {
                    continue;
                }

                totalRows++;

                int excelRow =
                        i + 1;

                String phone =
                        null;

                try {

                    ContactExcelData data =
                            readContactData(row);

                    phone =
                            normalizePhone(
                                    data.phone()
                            );

                    /*
                     * Validaciones.
                     */
                    validateContactData(
                            data,
                            phone
                    );

                    /*
                     * Detectar duplicados dentro
                     * del mismo Excel.
                     */
                    if (!phonesProcessed.add(phone)) {

                        skipped++;

                        errors.add(
                                ContactImportError.builder()
                                        .row(excelRow)
                                        .phone(phone)
                                        .message(
                                                "El teléfono aparece "
                                                + "más de una vez en "
                                                + "el archivo Excel."
                                        )
                                        .build()
                        );

                        continue;
                    }

                    /*
                     * Buscar contacto existente.
                     */
                    Contact contact =
                            contactRepository
                                    .findByPhone(phone)
                                    .orElse(null);

                    if (contact == null) {

                        /*
                         * Crear nuevo contacto.
                         */
                        contact =
                                createContact(
                                        data,
                                        phone
                                );

                        contactRepository.save(
                                contact
                        );

                        created++;

                    } else {

                        /*
                         * Actualizar contacto existente.
                         */
                        updateContact(
                                contact,
                                data,
                                phone
                        );

                        contactRepository.save(
                                contact
                        );

                        updated++;
                    }

                } catch (Exception exception) {

                    log.error(
                            "Error procesando fila {} del Excel",
                            excelRow,
                            exception
                    );

                    errors.add(
                            ContactImportError.builder()
                                    .row(excelRow)
                                    .phone(phone)
                                    .message(
                                            exception.getMessage()
                                                    != null
                                                    ? exception.getMessage()
                                                    : "Error procesando la fila."
                                    )
                                    .build()
                    );
                }
            }

        } catch (IOException exception) {

            throw new IllegalArgumentException(
                    "No fue posible leer el archivo Excel.",
                    exception
            );
        }

        return ContactImportResponse.builder()
                .totalRows(totalRows)
                .created(created)
                .updated(updated)
                .skipped(skipped)
                .errors(errors.size())
                .errorDetails(errors)
                .build();
    }

    // ============================================================
    // CREAR CONTACTO
    // ============================================================

    private Contact createContact(
            ContactExcelData data,
            String phone) {

        LocalDateTime now =
                LocalDateTime.now(
                        COLOMBIA_ZONE
                );

        return Contact.builder()
                .phone(phone)
                .name(data.name())
                .email(data.email())
                .company(data.company())
                .registrationStep(
                        determineRegistrationStep(
                                data.name(),
                                data.email(),
                                data.company()
                        )
                )
                .firstContact(now)
                .lastInteraction(null)
                .createdAt(now)
                .build();
    }

    // ============================================================
    // ACTUALIZAR CONTACTO
    // ============================================================

    private void updateContact(
            Contact contact,
            ContactExcelData data,
            String phone) {

        contact.setPhone(phone);

        /*
         * Solamente reemplazamos información
         * cuando el Excel realmente trae un valor.
         *
         * De esta forma no destruimos información
         * que ya existía en el CRM.
         */

        if (hasValue(data.name())) {

            contact.setName(
                    data.name()
            );
        }

        if (hasValue(data.email())) {

            contact.setEmail(
                    data.email()
            );
        }

        if (hasValue(data.company())) {

            contact.setCompany(
                    data.company()
            );
        }

        /*
         * Una vez actualizados los datos,
         * volvemos a determinar el estado.
         */
        contact.setRegistrationStep(
                determineRegistrationStep(
                        contact.getName(),
                        contact.getEmail(),
                        contact.getCompany()
                )
        );
    }

    // ============================================================
    // REGISTRATION STEP
    // ============================================================

    private RegistrationStep determineRegistrationStep(
            String name,
            String email,
            String company) {

        boolean hasName =
                hasValue(name);

        boolean hasEmail =
                hasValue(email);

        boolean hasCompany =
                hasValue(company);

        if (hasName
                && hasEmail
                && hasCompany) {

            return RegistrationStep.COMPLETED;
        }

        return RegistrationStep.EMAILANDCOMPANY;
    }

    // ============================================================
    // LEER FILA EXCEL
    // ============================================================

    private ContactExcelData readContactData(
            Row row) {

        String phone =
                getCellValue(
                        row,
                        0
                );

        String name =
                getCellValue(
                        row,
                        1
                );

        String email =
                getCellValue(
                        row,
                        2
                );

        String company =
                getCellValue(
                        row,
                        3
                );

        return new ContactExcelData(
                cleanValue(phone),
                cleanValue(name),
                cleanValue(email),
                cleanValue(company)
        );
    }

    // ============================================================
    // VALIDACIONES
    // ============================================================

    private void validateContactData(
            ContactExcelData data,
            String phone) {

        if (!hasValue(phone)) {

            throw new IllegalArgumentException(
                    "El teléfono es obligatorio."
            );
        }

        if (hasValue(data.email())
                && !isValidEmail(data.email())) {

            throw new IllegalArgumentException(
                    "El correo electrónico no tiene "
                    + "un formato válido."
            );
        }
    }

    private boolean isValidEmail(
            String email) {

        return email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        );
    }

    // ============================================================
    // NORMALIZACIÓN TELÉFONO
    // ============================================================

    private String normalizePhone(
            String phone) {

        if (!hasValue(phone)) {
            return null;
        }

        /*
         * Conservamos únicamente números.
         *
         * Ejemplo:
         *
         * +57 300 123 4567
         *
         * se convierte en:
         *
         * 573001234567
         */
        return phone
                .trim()
                .replaceAll("[^0-9]", "");
    }

    // ============================================================
    // ENCABEZADOS
    // ============================================================

    private void validateHeaders(
            Row headerRow) {

        if (headerRow == null) {

            throw new IllegalArgumentException(
                    "El archivo Excel no contiene encabezados."
            );
        }

        String phone =
                getCellValue(
                        headerRow,
                        0
                );

        String name =
                getCellValue(
                        headerRow,
                        1
                );

        String email =
                getCellValue(
                        headerRow,
                        2
                );

        String company =
                getCellValue(
                        headerRow,
                        3
                );

        if (!PHONE_HEADER.equalsIgnoreCase(phone)
                || !NAME_HEADER.equalsIgnoreCase(name)
                || !EMAIL_HEADER.equalsIgnoreCase(email)
                || !COMPANY_HEADER.equalsIgnoreCase(company)) {

            throw new IllegalArgumentException(
                    "Los encabezados del Excel deben ser: "
                    + "phone, name, email, company"
            );
        }
    }

    // ============================================================
    // ARCHIVO
    // ============================================================

    private void validateFile(
            MultipartFile file) {

        if (file == null
                || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un archivo Excel."
            );
        }

        String filename =
                file.getOriginalFilename();

        if (filename == null
                || !filename
                        .toLowerCase()
                        .endsWith(EXCEL_EXTENSION)) {

            throw new IllegalArgumentException(
                    "El archivo debe estar en formato .xlsx."
            );
        }
    }

    // ============================================================
    // UTILIDADES
    // ============================================================

    private String getCellValue(
            Row row,
            int columnIndex) {

        if (row == null) {
            return null;
        }

        Cell cell =
                row.getCell(columnIndex);

        if (cell == null) {
            return null;
        }

        return dataFormatter
                .formatCellValue(cell)
                .trim();
    }

    private String cleanValue(
            String value) {

        if (value == null) {
            return null;
        }

        value =
                value.trim();

        return value.isBlank()
                ? null
                : value;
    }

    private boolean hasValue(
            String value) {

        return value != null
                && !value.isBlank();
    }

    private boolean isEmptyRow(
            Row row) {

        if (row == null) {
            return true;
        }

        for (int i = 0; i < 4; i++) {

            String value =
                    getCellValue(
                            row,
                            i
                    );

            if (hasValue(value)) {
                return false;
            }
        }

        return true;
    }

    // ============================================================
    // RECORD INTERNO
    // ============================================================

    private record ContactExcelData(
            String phone,
            String name,
            String email,
            String company
    ) {
    }
}
