package com.neumatica.embudo.whatsap.services;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.repository.ContactRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExcelExportServices {

	@Autowired
	private ContactRepository contactRepository;
	
	public byte[] exportContactsExcel() {

        List<Contact> contacts = this.contactRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet hoja = workbook.createSheet("Contactos");

            // ======= ESTILO DEL ENCABEZADO =======

            CellStyle headerStyle = workbook.createCellStyle();

            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());

            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ======= CABECERA =======

            Row header = hoja.createRow(0);

            String[] columns = {
                    "Nombre",
                    "Teléfono",
                    "Correo",
                    "Fecha Registro"
            };

            for (int i = 0; i < columns.length; i++) {

                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);

            }

            // ======= DATOS =======

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            int fila = 1;

            for (Contact contact : contacts) {

                Row row = hoja.createRow(fila++);

                row.createCell(0).setCellValue(contact.getName());

                row.createCell(1).setCellValue(contact.getPhone());

                row.createCell(2).setCellValue(contact.getEmail());

                row.createCell(3).setCellValue(
                        contact.getCreatedAt().format(formatter)
                );

            }

            // Ajustar ancho automáticamente

            for (int i = 0; i < columns.length; i++) {

                hoja.autoSizeColumn(i);

            }

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException("Error generando Excel", e);

        }

    }
}
