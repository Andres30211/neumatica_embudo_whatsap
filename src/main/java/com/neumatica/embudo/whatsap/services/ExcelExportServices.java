package com.neumatica.embudo.whatsap.services;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.HeaderFooter;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
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
	
	/*public byte[] exportContactsExcel() {

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

    }*/
	
	public byte[] exportContactsExcel() {

	    List<Contact> contacts = contactRepository.findAll();

	    try (Workbook workbook = new XSSFWorkbook();
	         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

	        Sheet sheet = workbook.createSheet("Contactos");

	        sheet.setDisplayGridlines(false);

	        //--------------------------------------------------
	        // FUENTES
	        //--------------------------------------------------

	        Font titleFont = workbook.createFont();
	        titleFont.setBold(true);
	        titleFont.setFontHeightInPoints((short)18);
	        titleFont.setColor(IndexedColors.WHITE.getIndex());

	        Font subtitleFont = workbook.createFont();
	        subtitleFont.setBold(true);
	        subtitleFont.setFontHeightInPoints((short)12);

	        Font headerFont = workbook.createFont();
	        headerFont.setBold(true);
	        headerFont.setColor(IndexedColors.WHITE.getIndex());

	        Font normalFont = workbook.createFont();
	        normalFont.setFontHeightInPoints((short)10);

	        //--------------------------------------------------
	        // ESTILOS
	        //--------------------------------------------------

	        CellStyle titleStyle = workbook.createCellStyle();

	        titleStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
	        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	        titleStyle.setAlignment(HorizontalAlignment.CENTER);
	        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
	        titleStyle.setFont(titleFont);

	        CellStyle infoStyle = workbook.createCellStyle();

	        infoStyle.setFont(normalFont);
	        infoStyle.setBorderBottom(BorderStyle.THIN);

	        CellStyle headerStyle = workbook.createCellStyle();

	        headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
	        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	        headerStyle.setAlignment(HorizontalAlignment.CENTER);
	        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
	        headerStyle.setBorderBottom(BorderStyle.THICK);
	        headerStyle.setFont(headerFont);

	        CellStyle evenStyle = workbook.createCellStyle();
	        evenStyle.setBorderBottom(BorderStyle.THIN);

	        CellStyle oddStyle = workbook.createCellStyle();
	        oddStyle.setBorderBottom(BorderStyle.THIN);

	        oddStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
	        oddStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

	        CellStyle summaryStyle = workbook.createCellStyle();

	        summaryStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
	        summaryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	        summaryStyle.setBorderTop(BorderStyle.MEDIUM);
	        summaryStyle.setFont(subtitleFont);

	        //--------------------------------------------------
	        // TITULO
	        //--------------------------------------------------

	        sheet.addMergedRegion(new CellRangeAddress(0,0,0,5));

	        Row rowTitle = sheet.createRow(0);

	        rowTitle.setHeightInPoints(30);

	        Cell title = rowTitle.createCell(0);

	        title.setCellValue("REPORTE DE CONTACTOS WHATSAPP");

	        title.setCellStyle(titleStyle);

	        //--------------------------------------------------
	        // EMPRESA
	        //--------------------------------------------------

	        sheet.addMergedRegion(new CellRangeAddress(1,1,0,5));

	        Row company = sheet.createRow(1);

	        Cell companyCell = company.createCell(0);

	        companyCell.setCellValue("Mi Empresa S.A.S.");

	        companyCell.setCellStyle(infoStyle);

	        //--------------------------------------------------
	        // INFORMACIÓN
	        //--------------------------------------------------

	        DateTimeFormatter formatter =
	                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	        String[][] info = {

	                {"Empresa", "Mi Empresa S.A.S."},
	                {"Sistema", "WhatsApp API"},
	                {"Generado", formatter.format(LocalDateTime.now())},
	                {"Usuario", "Administrador"},
	                {"Total contactos", String.valueOf(contacts.size())}

	        };

	        int rowInfo = 3;

	        for(String[] item : info){

	            Row row = sheet.createRow(rowInfo++);

	            Cell c1 = row.createCell(0);
	            c1.setCellValue(item[0]);

	            Cell c2 = row.createCell(1);
	            c2.setCellValue(item[1]);

	        }

	        //--------------------------------------------------
	        // CABECERA TABLA
	        //--------------------------------------------------

	        int startTable = 10;

	        Row header = sheet.createRow(startTable);

	        String[] columns = {

	                "Nombre",
	                "Teléfono",
	                "Correo",
	                "Fecha Registro"

	        };

	        for(int i=0;i<columns.length;i++){

	            Cell cell = header.createCell(i);

	            cell.setCellValue(columns[i]);

	            cell.setCellStyle(headerStyle);

	        }

	        //--------------------------------------------------
	        // DATOS
	        //--------------------------------------------------

	        int rowNumber = startTable + 1;

	        DateTimeFormatter dateFormatter =
	                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	        for(Contact contact : contacts){

	            Row row = sheet.createRow(rowNumber);

	            CellStyle style =
	                    rowNumber % 2 == 0
	                            ? evenStyle
	                            : oddStyle;

	            Cell c0 = row.createCell(0);
	            c0.setCellValue(contact.getName());
	            c0.setCellStyle(style);

	            Cell c1 = row.createCell(1);
	            c1.setCellValue(contact.getPhone());
	            c1.setCellStyle(style);

	            Cell c2 = row.createCell(2);
	            c2.setCellValue(contact.getEmail());
	            c2.setCellStyle(style);

	            Cell c3 = row.createCell(3);

	            c3.setCellValue(
	                    contact.getCreatedAt().format(dateFormatter));

	            c3.setCellStyle(style);

	            rowNumber++;

	        }

	        //--------------------------------------------------
	        // RESUMEN
	        //--------------------------------------------------

	        rowNumber += 2;

	        Row summaryTitle = sheet.createRow(rowNumber++);

	        Cell summaryCell = summaryTitle.createCell(0);

	        summaryCell.setCellValue("RESUMEN");

	        summaryCell.setCellStyle(summaryStyle);

	        Row total = sheet.createRow(rowNumber++);

	        total.createCell(0).setCellValue("Total contactos");

	        total.createCell(1).setCellValue(contacts.size());

	        long emails = contacts.stream()
	                .filter(c -> c.getEmail()!=null && !c.getEmail().isBlank())
	                .count();

	        Row mail = sheet.createRow(rowNumber++);

	        mail.createCell(0).setCellValue("Con correo");

	        mail.createCell(1).setCellValue(emails);

	        //--------------------------------------------------
	        // FILTROS
	        //--------------------------------------------------

	        sheet.setAutoFilter(
	                new CellRangeAddress(
	                        startTable,
	                        startTable,
	                        0,
	                        columns.length-1));

	        //--------------------------------------------------
	        // CONGELAR CABECERA
	        //--------------------------------------------------

	        sheet.createFreezePane(0,startTable+1);

	        //--------------------------------------------------
	        // FOOTER
	        //--------------------------------------------------

	        Footer footer = sheet.getFooter();

	        footer.setLeft("© 2026 Mi Empresa S.A.S.");
	        footer.setCenter("Reporte generado automáticamente por WhatsApp API");
	        footer.setRight("Página &P de &N");

	        //--------------------------------------------------
	        // ANCHOS
	        //--------------------------------------------------

	        for(int i=0;i<columns.length;i++){

	            sheet.autoSizeColumn(i);

	            sheet.setColumnWidth(
	                    i,
	                    sheet.getColumnWidth(i)+1200);

	        }

	        workbook.write(outputStream);

	        return outputStream.toByteArray();

	    } catch (Exception e) {

	        throw new RuntimeException(e);

	    }

	}
}
