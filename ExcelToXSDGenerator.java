import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;

public class ExcelToXSDGenerator {

    public static void main(String[] args) throws Exception {

        String excelFile = "fields.xlsx";
        String xsdFile = "output.xsd";

        Workbook workbook = new XSSFWorkbook(new FileInputStream(excelFile));
        Sheet sheet = workbook.getSheetAt(0);

        StringBuilder xsd = new StringBuilder();

        // XSD Header
        xsd.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xsd.append("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n");
        xsd.append("  <xs:element name=\"Root\">\n");
        xsd.append("    <xs:complexType>\n");
        xsd.append("      <xs:sequence>\n");

        // Skip header row (row 0)
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);
            if (row == null) continue;

            String fieldName = row.getCell(0).getStringCellValue().trim();
            String maxType = row.getCell(1).getStringCellValue().trim();
            String mandatory = row.getCell(2).getStringCellValue().trim();

            String minOccurs = mandatory.equalsIgnoreCase("Yes") ? "1" : "0";

            xsd.append("        <xs:element name=\"")
               .append(fieldName)
               .append("\" minOccurs=\"")
               .append(minOccurs)
               .append("\">\n");

            xsd.append("          <xs:simpleType>\n");

            if (maxType.contains("Text")) {
                String length = maxType.replace("Max", "").replace("Text", "");
                xsd.append("            <xs:restriction base=\"xs:string\">\n");
                xsd.append("              <xs:maxLength value=\"")
                   .append(length)
                   .append("\"/>\n");
                xsd.append("            </xs:restriction>\n");

            } else if (maxType.contains("DecimalNumber")) {
                String digits = maxType.replace("Max", "").replace("DecimalNumber", "");
                xsd.append("            <xs:restriction base=\"xs:decimal\">\n");
                xsd.append("              <xs:totalDigits value=\"")
                   .append(digits)
                   .append("\"/>\n");
                xsd.append("            </xs:restriction>\n");
            }

            xsd.append("          </xs:simpleType>\n");
            xsd.append("        </xs:element>\n");
        }

        // XSD Footer
        xsd.append("      </xs:sequence>\n");
        xsd.append("    </xs:complexType>\n");
        xsd.append("  </xs:element>\n");
        xsd.append("</xs:schema>");

        workbook.close();

        // Write XSD to file
        FileWriter writer = new FileWriter(xsdFile);
        writer.write(xsd.toString());
        writer.close();

        System.out.println("XSD generated successfully: " + xsdFile);
    }
}
