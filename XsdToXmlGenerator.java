import com.sun.xml.xsom.*;
import com.sun.xml.xsom.parser.XSOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class XsdToXmlGenerator {

    public enum GenerationMode {
        ALL_TAGS,
        MANDATORY_ONLY
    }

    public static void main(String[] args) {
        try {

            String xsdPath = "sample.xsd";
            String outputXml = "output.xml";

            // OPTION 1 -> ALL_TAGS
            generateXmlFromXsd(xsdPath, outputXml, GenerationMode.ALL_TAGS);

            // OPTION 2 -> MANDATORY_ONLY
            // generateXmlFromXsd(xsdPath, outputXml, GenerationMode.MANDATORY_ONLY);

            System.out.println("XML Generated Successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateXmlFromXsd(String xsdPath,
                                          String outputXml,
                                          GenerationMode mode) throws Exception {

        // Parse XSD
        XSOMParser parser = new XSOMParser();
        parser.parse(new File(xsdPath));

        XSSchemaSet schemaSet = parser.getResult();

        if (schemaSet == null) {
            throw new RuntimeException("Failed to parse XSD.");
        }

        // Create XML Document
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document document = docBuilder.newDocument();

        // Get Root Element
        XSElementDecl rootElement = null;

        for (XSSchema schema : schemaSet.getSchemas()) {
            for (XSElementDecl elementDecl : schema.getElementDecls().values()) {
                rootElement = elementDecl;
                break;
            }
            if (rootElement != null) {
                break;
            }
        }

        if (rootElement == null) {
            throw new RuntimeException("No root element found in XSD.");
        }

        // Generate XML
        Element root = document.createElement(rootElement.getName());
        document.appendChild(root);

        processElement(rootElement, root, document, mode);

        // Write XML to file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(new File(outputXml));

        transformer.transform(source, result);
    }

    private static void processElement(XSElementDecl elementDecl,
                                       Element parentElement,
                                       Document document,
                                       GenerationMode mode) {

        XSType xsType = elementDecl.getType();

        if (xsType.isComplexType()) {

            XSComplexType complexType = xsType.asComplexType();
            XSParticle particle = complexType.getContentType().asParticle();

            if (particle != null) {
                processParticle(particle, parentElement, document, mode);
            }

        } else {
            // Simple element value
            parentElement.setTextContent("sample");
        }
    }

    private static void processParticle(XSParticle particle,
                                        Element parentElement,
                                        Document document,
                                        GenerationMode mode) {

        XSTerm term = particle.getTerm();

        if (term.isModelGroup()) {

            XSModelGroup modelGroup = term.asModelGroup();

            for (XSParticle childParticle : modelGroup.getChildren()) {

                XSTerm childTerm = childParticle.getTerm();

                if (childTerm.isElementDecl()) {

                    XSElementDecl childElement = childTerm.asElementDecl();

                    boolean isMandatory = childParticle.getMinOccurs().intValue() > 0;

                    // Skip optional tags in MANDATORY_ONLY mode
                    if (mode == GenerationMode.MANDATORY_ONLY && !isMandatory) {
                        continue;
                    }

                    Element childXmlElement =
                            document.createElement(childElement.getName());

                    parentElement.appendChild(childXmlElement);

                    processElement(childElement,
                            childXmlElement,
                            document,
                            mode);
                }
            }
        }
    }
}
