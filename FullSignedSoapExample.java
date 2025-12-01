import org.apache.xml.security.c14n.Canonicalizer;
import javax.xml.soap.*;
import java.security.*;
import java.util.Base64;

public class FullSignedSoapExample {

    public static void main(String[] args) throws Exception {

        org.apache.xml.security.Init.init(); // Initialize XMLSec library

        // -------------------------
        // 1) CREATE SOAP MESSAGE
        // -------------------------
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage message = factory.createMessage();
        SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
        SOAPBody body = envelope.getBody();

        SOAPElement req = body.addChildElement("TestRequest", "ns1", "http://demo/");
        req.addChildElement("Message").setTextContent("Hello World");

        message.saveChanges();

        String bodyXml = nodeToString(body);

        // -------------------------
        // 2) CREATE DIGEST VALUE
        // -------------------------
        byte[] canonBody = Canonicalizer.getInstance(
                Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS
        ).canonicalize(bodyXml.getBytes());

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String digestValue = Base64.getEncoder().encodeToString(md.digest(canonBody));

        System.out.println("DigestValue = " + digestValue);


        // -------------------------
        // 3) GENERATE RSA KEYPAIR
        // -------------------------
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair pair = kpg.generateKeyPair();

        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();


        // -------------------------
        // 4) BUILD SIGNEDINFO XML
        // -------------------------
        String signedInfoXml =
                "<SignedInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">" +
                        "<CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>" +
                        "<SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/>" +
                        "<Reference URI=\"#Body\">" +
                        "<Transforms><Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></Transforms>" +
                        "<DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>" +
                        "<DigestValue>" + digestValue + "</DigestValue>" +
                        "</Reference>" +
                        "</SignedInfo>";

        // Canonicalize SignedInfo
        byte[] canonSignedInfo = Canonicalizer.getInstance(
                Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS
        ).canonicalize(signedInfoXml.getBytes());


        // -------------------------
        // 5) SIGN SIGNEDINFO
        // -------------------------
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(canonSignedInfo);

        String signatureValue = Base64.getEncoder().encodeToString(signature.sign());
        System.out.println("SignatureValue = " + signatureValue);


        // -------------------------
        // 6) BUILD FINAL SIGNED SOAP
        // -------------------------
        String finalSoap =
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" " +
                        "xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">" +

                        "<soap:Header>" +
                        "<wsse:Security>" +
                        "<ds:Signature>" +
                        signedInfoXml +
                        "<SignatureValue>" + signatureValue + "</SignatureValue>" +
                        "</ds:Signature>" +
                        "</wsse:Security>" +
                        "</soap:Header>" +

                        "<soap:Body Id=\"Body\">" +
                        "<ns1:TestRequest xmlns:ns1=\"http://demo/\">" +
                        "<Message>Hello World</Message>" +
                        "</ns1:TestRequest>" +
                        "</soap:Body>" +
                        "</soap:Envelope>";

        System.out.println("\n========== FINAL SIGNED SOAP ========== \n");
        System.out.println(finalSoap);
    }

    // Helper function to convert DOM element → String
    public static String nodeToString(SOAPBody body) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        body.writeTo(baos);
        return baos.toString();
    }
}
