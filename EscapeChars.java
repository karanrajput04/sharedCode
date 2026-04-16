import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.*;

public class XmlValueEscaper {

    public static String escapeXmlValues(String xmlInput) throws Exception {

        // Step 1: Parse XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlInput.getBytes("UTF-8")));

        // Step 2: Process nodes
        processNode(doc);

        // Step 3: Convert back to String
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        return writer.toString();
    }

    private static void processNode(Node node) {

        // Escape text node values
        if (node.getNodeType() == Node.TEXT_NODE) {
            String original = node.getTextContent();
            if (original != null && !original.trim().isEmpty()) {
                node.setTextContent(StringEscapeUtils.escapeXml11(original));
            }
        }

        // Escape attribute values
        if (node.hasAttributes()) {
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                String value = attr.getNodeValue();
                if (value != null && !value.trim().isEmpty()) {
                    attr.setNodeValue(StringEscapeUtils.escapeXml11(value));
                }
            }
        }

        // Recursively process child nodes
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            processNode(children.item(i));
        }
    }

    // Sample test
    public static void main(String[] args) throws Exception {

        String input = "<root><tag attr=\"5 < 10 & test\">Hello & welcome <user></tag></root>";

        String output = escapeXmlValues(input);

        System.out.println("Original:\n" + input);
        System.out.println("\nEscaped:\n" + output);
    }
}
