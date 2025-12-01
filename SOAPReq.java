import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.*;
import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class SoapSigner {

    public static void main(String[] args) throws Exception {

        // ============================================================
        // 1. Load Keystore (Client Certificate + Private Key)
        // ============================================================
        String keystorePath = "C:/certs/client.p12";
        String keystorePass = "changeit";
        String keyAlias = "mykeyalias";

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream(keystorePath), keystorePass.toCharArray());

        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keystorePass.toCharArray());
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(keyAlias);

        // ============================================================
        // 2. Truststore (Server Certificate)
        // ============================================================
        System.setProperty("javax.net.ssl.trustStore", "C:/certs/truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

        // ============================================================
        // 3. Your SOAP request string
        // ============================================================
        String soapRequest =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                        "<soapenv:Header/>" +
                        "<soapenv:Body>" +
                        "   <ns2:YourRequest xmlns:ns2=\"http://example.com\">" +
                        "       <value>123</value>" +
                        "   </ns2:YourRequest>" +
                        "</soapenv:Body>" +
                        "</soapenv:Envelope>";

        // ============================================================
        // 4. Convert String → XML Document
        // ============================================================
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(soapRequest.getBytes()));

        // ============================================================
        // 5. Create XML Signature Factory
        // ============================================================
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // ============================================================
        // 6. Locate SOAP Body (digest will be taken for this)
        // ============================================================
        NodeList bodies = doc.getElementsByTagNameNS("*", "Body");

        if (bodies.getLength() == 0)
            throw new RuntimeException("SOAP Body not found!");

        org.w3c.dom.Node bodyNode = bodies.item(0);

        // Reference for signing SOAP Body
        Reference ref = fac.newReference(
                "",  // empty URI → whole document or specific node via DOMSignContext
                fac.newDigestMethod(DigestMethod.SHA256, null),
                null,
                null,
                null
        );

        // SignedInfo → Canonicalization + Signature algo
        SignedInfo signedInfo = fac.newSignedInfo(
                fac.newCanonicalizationMethod(
                        CanonicalizationMethod.INCLUSIVE,
                        (C14NMethodParameterSpec) null
                ),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                java.util.Collections.singletonList(ref)
        );

        // KeyInfo (contains public certificate)
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509Data x509data = kif.newX509Data(java.util.Collections.singletonList(cert));
        KeyInfo keyInfo = kif.newKeyInfo(java.util.Collections.singletonList(x509data));

        // ============================================================
        // 7. Prepare signature element inside <SOAP-Header>
        // ============================================================
        NodeList headers = doc.getElementsByTagNameNS("*", "Header");
        org.w3c.dom.Node headerNode = headers.item(0);

        DOMSignContext signContext = new DOMSignContext(privateKey, headerNode);

        // Set which node is referenced for digest
        signContext.setDefaultNamespacePrefix("ds");

        // ============================================================
        // 8. Create and Insert Digital Signature
        // ============================================================
        XMLSignature signature = fac.newXMLSignature(signedInfo, keyInfo);
        signature.sign(signContext);

        // ============================================================
        // 9. Convert back to string
        // ============================================================
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter sw = new StringWriter();
        tf.transform(new DOMSource(doc), new StreamResult(sw));

        System.out.println("\n========= FINAL SIGNED SOAP REQUEST =========\n");
        System.out.println(sw.toString());
    }
}
