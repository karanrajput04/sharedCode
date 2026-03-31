import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class EnrichmentValidatorTest {

    // ============================
    // 🔹 MAIN METHOD (TEST ENTRY)
    // ============================
    public static void main(String[] args) throws Exception {

        String xmlPath = "input.xml";
        String xsdPath = "pacs.008.xsd";
        String enrichPath = "enrich_tag.txt";
        String outputPath = "output.xml";

        // Load XML
        Document doc = loadXml(xmlPath);

        // Load XPath list
       ///Document/FIToFICstmrCdtTrf/CdtTrfTxInf/Cdtr/PstlAdr/AdrLine
       ///Document/FIToFICstmrCdtTrf/CdtTrfTxInf/Cdtr/Nm
        List<String> rawPaths = Files.readAllLines(Paths.get(enrichPath));

        // Parse XSD
        Map<String, Integer> typeMaxLengthMap = extractMaxLengthFromXsd(xsdPath);
        Map<String, String> elementTypeMap = extractElementTypeMap(xsdPath);

        // Build final tag → maxLength map
        Map<String, Integer> tagMaxLengthMap = new HashMap<>();
        for (String tag : elementTypeMap.keySet()) {
            String type = elementTypeMap.get(tag);
            if (typeMaxLengthMap.containsKey(type)) {
                tagMaxLengthMap.put(tag, typeMaxLengthMap.get(type));
            }
        }

        XPath xpath = XPathFactory.newInstance().newXPath();

        // ============================
        // 🔥 PROCESS EACH XPATH
        // ============================
        for (String rawPath : rawPaths) {

            if (rawPath == null || rawPath.trim().isEmpty()) continue;

            String convertedXPath = toLocalNameXPath(rawPath);

            NodeList nodes = (NodeList) xpath.evaluate(
                    convertedXPath, doc, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); i++) {

                Element el = (Element) nodes.item(i);

                String tagName = el.getLocalName();
                String value = el.getTextContent();

                if (value == null) continue;

                Integer maxLength = tagMaxLengthMap.get(tagName);

                if (maxLength != null && value.length() > maxLength) {

                    String trimmed = value.substring(0, maxLength);
                    el.setTextContent(trimmed);

                    System.out.println("⚠ Trimmed [" + rawPath + "] "
                            + value.length() + " → " + maxLength);
                }
            }
        }

        // Save output
        writeXml(doc, outputPath);

        System.out.println("✅ Processing completed. Output: " + outputPath);
    }

    // ============================
    // 🔹 Convert Normal XPath → Namespace Safe
    // ============================
    private static String toLocalNameXPath(String inputPath) {

        String[] parts = inputPath.split("/");
        StringBuilder xpath = new StringBuilder();

        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) continue;

            xpath.append("/*[local-name()='")
                    .append(part.trim())
                    .append("']");
        }

        return xpath.toString();
    }

    // ============================
    // 🔹 Extract maxLength from XSD
    // ============================
    private static Map<String, Integer> extractMaxLengthFromXsd(String xsdPath) throws Exception {

        Map<String, Integer> map = new HashMap<>();

        Document doc = loadXml(xsdPath);

        NodeList simpleTypes = doc.getElementsByTagName("xs:simpleType");

        for (int i = 0; i < simpleTypes.getLength(); i++) {

            Element simpleType = (Element) simpleTypes.item(i);
            String typeName = simpleType.getAttribute("name");

            NodeList maxNodes = simpleType.getElementsByTagName("xs:maxLength");

            if (maxNodes.getLength() > 0) {
                Element max = (Element) maxNodes.item(0);
                int value = Integer.parseInt(max.getAttribute("value"));

                map.put(typeName, value);
            }
        }

        return map;
    }

    // ============================
    // 🔹 Map Element → Type
    // ============================
    private static Map<String, String> extractElementTypeMap(String xsdPath) throws Exception {

        Map<String, String> map = new HashMap<>();

        Document doc = loadXml(xsdPath);

        NodeList elements = doc.getElementsByTagName("xs:element");

        for (int i = 0; i < elements.getLength(); i++) {

            Element el = (Element) elements.item(i);

            String name = el.getAttribute("name");
            String type = el.getAttribute("type");

            if (name != null && !name.isEmpty() &&
                type != null && !type.isEmpty()) {

                map.put(name, type);
            }
        }

        return map;
    }

    // ============================
    // 🔹 Load XML/XSD
    // ============================
    private static Document loadXml(String path) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        return factory.newDocumentBuilder().parse(new File(path));
    }

    // ============================
    // 🔹 Write XML Output
    // ============================
    private static void writeXml(Document doc, String outputPath) throws Exception {

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(outputPath));

        transformer.transform(source, result);
    }
}
