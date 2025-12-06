import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal, runnable MT103 (Blocks 1-4) -> pacs.008 XML demo in core Java.
 * Simplified: only subset of tags and pacs.008 fields for illustration.
 */
public class MtToPacsDemo {

    // ---------- Domain models ----------

    static class MtBlock1 {
        char appId;
        String serviceId;
        String logicalTerminal;
        String sessionNumber;
        String sequenceNumber;
    }

    static class MtBlock2 {
        char ioId;
        String messageType;
        String bic;
        String priority;
    }

    static class MtBlock3 {
        String uetr121;
        String mir108;
        String stp119;
        Map<String, String> otherTags = new LinkedHashMap<>();
    }

    static class Mt103Block4 {
        String t20;
        String t23B;
        String raw32A;
        String t50K;
        String t59;
        String t70;
        String t71A;

        LocalDate valueDate;
        String currency;
        BigDecimal interbankAmount;
    }

    static class Mt103Envelope {
        MtBlock1 block1;
        MtBlock2 block2;
        MtBlock3 block3;
        Mt103Block4 block4;
    }

    // pacs.008 models (simplified)
    static class Pacs008 {
        static class GroupHeader {
            String msgId;
            LocalDateTime creDtTm;
            String nbOfTxs;
            BigDecimal ctrlSum;
            Agent instgAgt;
            Agent instdAgt;
        }

        static class FinancialInstitutionId {
            String bic;
        }

        static class BranchAndFiId {
            FinancialInstitutionId finInstnId;
        }

        static class Agent {
            BranchAndFiId finInstn;
        }

        static class Account {
            String iban;
            String otherId;
        }

        static class PostalAddress {
            String country;
            List<String> addressLines = new ArrayList<>();
        }

        static class Party {
            String name;
            PostalAddress postalAddress;
            Account account;
        }

        static class PaymentId {
            String endToEndId;
            String txId;
            String uetr;
        }

        static class CreditTransferTx {
            PaymentId pmtId;
            BigDecimal intrBkSttlmAmt;
            String intrBkSttlmCcy;
            LocalDate intrBkSttlmDt;
            Party debtor;
            Agent debtorAgent;
            Agent creditorAgent;
            Party creditor;
            String remittanceInfoUnstructured;
        }

        GroupHeader grpHdr;
        List<CreditTransferTx> txList = new ArrayList<>();
    }

    // ---------- Parsers ----------

    static class MtParser {

        private static final Pattern FIELD_PATTERN =
                Pattern.compile(":(\\d{2}[A-Z]?):");

        public Mt103Envelope parse(String fin) {
            Mt103Envelope env = new Mt103Envelope();
            env.block1 = parseBlock1(fin);
            env.block2 = parseBlock2(fin);
            env.block3 = parseBlock3(fin);
            env.block4 = parseBlock4(fin);
            return env;
        }

        private MtBlock1 parseBlock1(String fin) {
            int start = fin.indexOf("{1:");
            int end = fin.indexOf("}", start);
            if (start < 0 || end < 0) throw new IllegalArgumentException("Block 1 not found");
            String b = fin.substring(start + 3, end); // F01BANKBEBBAXXX2222123456

            MtBlock1 block1 = new MtBlock1();
            block1.appId = b.charAt(0);
            block1.serviceId = b.substring(1, 3);
            block1.logicalTerminal = b.substring(3, 15);
            block1.sessionNumber = b.substring(15, 19);
            block1.sequenceNumber = b.substring(19);
            return block1;
        }

        private MtBlock2 parseBlock2(String fin) {
            int start = fin.indexOf("{2:");
            int end = fin.indexOf("}", start);
            if (start < 0 || end < 0) throw new IllegalArgumentException("Block 2 not found");
            String b = fin.substring(start + 3, end);

            MtBlock2 block2 = new MtBlock2();
            // Example: O1031201050103BANKDEFFXXXX22221234560501031201N
            block2.ioId = b.charAt(0);
            block2.messageType = b.substring(1, 4);
            // for simplicity assume "O103...BIC12...N"
            // find BIC at position 15..27 in this demo
            if (b.length() >= 27) {
                block2.bic = b.substring(15, 27);
            }
            block2.priority = String.valueOf(b.charAt(b.length() - 1));
            return block2;
        }

        private MtBlock3 parseBlock3(String fin) {
            MtBlock3 b3 = new MtBlock3();
            int start = fin.indexOf("{3:");
            if (start < 0) return b3; // optional

            int end = fin.indexOf("}", start);
            if (end < 0) return b3;

            String block = fin.substring(start + 3, end); // {121:..}{108:..}
            Pattern inner = Pattern.compile("\\{(\\d{3}):([^}]*)}");
            Matcher m = inner.matcher(block);
            while (m.find()) {
                String tag = m.group(1);
                String val = m.group(2);
                b3.otherTags.put(tag, val);
                if ("121".equals(tag)) b3.uetr121 = val;
                if ("108".equals(tag)) b3.mir108 = val;
                if ("119".equals(tag)) b3.stp119 = val;
            }
            return b3;
        }

        private Mt103Block4 parseBlock4(String fin) {
            int start = fin.indexOf("{4:");
            int end = fin.indexOf("-}", start);
            if (start < 0 || end < 0) throw new IllegalArgumentException("Block 4 not found");
            String block4 = fin.substring(start + 3, end);

            Map<String, String> fields = parseFields(block4);
            return toMt103Block4(fields);
        }

        private Map<String, String> parseFields(String block4) {
            Map<String, String> map = new LinkedHashMap<>();
            Matcher m = FIELD_PATTERN.matcher(block4);
            int lastPos = 0;
            String lastTag = null;

            while (m.find()) {
                if (lastTag != null) {
                    String value = block4.substring(lastPos, m.start());
                    map.put(lastTag, value.trim());
                }
                lastTag = m.group(1);
                lastPos = m.end();
            }
            if (lastTag != null && lastPos < block4.length()) {
                map.put(lastTag, block4.substring(lastPos).trim());
            }
            return map;
        }

        private Mt103Block4 toMt103Block4(Map<String, String> fields) {
            Mt103Block4 mt = new Mt103Block4();
            mt.t20 = fields.get("20");
            mt.t23B = fields.get("23B");
            mt.raw32A = fields.get("32A");
            mt.t50K = fields.get("50K");
            mt.t59 = fields.get("59");
            mt.t70 = fields.get("70");
            mt.t71A = fields.get("71A");

            if (mt.raw32A != null && mt.raw32A.length() >= 14) {
                String dateStr = mt.raw32A.substring(0, 6);
                String ccy = mt.raw32A.substring(6, 9);
                String amtStr = mt.raw32A.substring(9).replace(',', '.');
                LocalDate date = LocalDate.parse("20" + dateStr,
                        DateTimeFormatter.ofPattern("yyyyMMdd"));
                mt.valueDate = date;
                mt.currency = ccy;
                mt.interbankAmount = new BigDecimal(amtStr);
            }
            return mt;
        }
    }

    // ---------- Mapper MT -> pacs.008 ----------

    static class MtToPacsMapper {

        public Pacs008 map(Mt103Envelope env) {
            Pacs008 mx = new Pacs008();

            Pacs008.GroupHeader hdr = new Pacs008.GroupHeader();
            hdr.msgId = env.block3.uetr121 != null ? env.block3.uetr121 : env.block4.t20;
            hdr.creDtTm = LocalDateTime.now();
            hdr.nbOfTxs = "1";
            hdr.ctrlSum = env.block4.interbankAmount;

            hdr.instgAgt = makeAgent(senderBic(env.block1, env.block2));
            hdr.instdAgt = makeAgent(receiverBic(env.block1, env.block2));

            mx.grpHdr = hdr;

            Pacs008.CreditTransferTx tx = new Pacs008.CreditTransferTx();
            tx.intrBkSttlmAmt = env.block4.interbankAmount;
            tx.intrBkSttlmCcy = env.block4.currency;
            tx.intrBkSttlmDt = env.block4.valueDate;

            Pacs008.PaymentId pmtId = new Pacs008.PaymentId();
            pmtId.uetr = env.block3.uetr121;
            pmtId.endToEndId = env.block4.t20;
            pmtId.txId = env.block3.mir108 != null ? env.block3.mir108 : env.block4.t20;
            tx.pmtId = pmtId;

            tx.debtor = parseParty(env.block4.t50K);
            tx.creditor = parseParty(env.block4.t59);
            tx.remittanceInfoUnstructured = env.block4.t70;

            mx.txList.add(tx);
            return mx;
        }

        private Pacs008.Agent makeAgent(String bic) {
            if (bic == null) return null;
            Pacs008.FinancialInstitutionId fi = new Pacs008.FinancialInstitutionId();
            fi.bic = bic;
            Pacs008.BranchAndFiId br = new Pacs008.BranchAndFiId();
            br.finInstnId = fi;
            Pacs008.Agent ag = new Pacs008.Agent();
            ag.finInstn = br;
            return ag;
        }

        private String senderBic(MtBlock1 b1, MtBlock2 b2) {
            if (b2.ioId == 'I') {
                return bicFromLt(b1.logicalTerminal);
            } else {
                return b2.bic;
            }
        }

        private String receiverBic(MtBlock1 b1, MtBlock2 b2) {
            if (b2.ioId == 'I') {
                return b2.bic;
            } else {
                return bicFromLt(b1.logicalTerminal);
            }
        }

        private String bicFromLt(String lt) {
            if (lt == null || lt.length() < 12) return null;
            return lt.substring(0, 8) + lt.substring(8, 11);
        }

        private Pacs008.Party parseParty(String field) {
            if (field == null) return null;
            Pacs008.Party p = new Pacs008.Party();
            Pacs008.Account acct = new Pacs008.Account();
            Pacs008.PostalAddress adr = new Pacs008.PostalAddress();

            String[] lines = field.split("\\r?\\n");
            int idx = 0;
            if (lines.length > 0 && lines[0].startsWith("/")) {
                acct.otherId = lines[0].substring(1);
                idx = 1;
            }
            if (idx < lines.length) {
                p.name = lines[idx];
                idx++;
            }
            for (; idx < lines.length; idx++) {
                adr.addressLines.add(lines[idx]);
            }
            p.account = acct;
            p.postalAddress = adr;
            return p;
        }
    }

    // ---------- XML builder for pacs.008 ----------

    static class Pacs008XmlBuilder {

        private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

        public String toXml(Pacs008 mx) throws Exception {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            Element document = doc.createElementNS(NS, "Document");
            doc.appendChild(document);

            Element root = doc.createElementNS(NS, "FIToFICstmrCdtTrf");
            document.appendChild(root);

            // Group header
            Element grpHdr = doc.createElementNS(NS, "GrpHdr");
            root.appendChild(grpHdr);
            addText(doc, grpHdr, "MsgId", mx.grpHdr.msgId);
            addText(doc, grpHdr, "CreDtTm", mx.grpHdr.creDtTm.toString());
            addText(doc, grpHdr, "NbOfTxs", mx.grpHdr.nbOfTxs);
            addText(doc, grpHdr, "CtrlSum", mx.grpHdr.ctrlSum.toPlainString());

            if (mx.grpHdr.instgAgt != null) {
                Element instg = doc.createElementNS(NS, "InstgAgt");
                grpHdr.appendChild(instg);
                addBicAgent(doc, instg, mx.grpHdr.instgAgt);
            }
            if (mx.grpHdr.instdAgt != null) {
                Element instd = doc.createElementNS(NS, "InstdAgt");
                grpHdr.appendChild(instd);
                addBicAgent(doc, instd, mx.grpHdr.instdAgt);
            }

            // Transactions (only one in this demo)
            for (Pacs008.CreditTransferTx tx : mx.txList) {
                Element cdtTrfTxInf = doc.createElementNS(NS, "CdtTrfTxInf");
                root.appendChild(cdtTrfTxInf);

                Element pmtId = doc.createElementNS(NS, "PmtId");
                cdtTrfTxInf.appendChild(pmtId);
                if (tx.pmtId != null) {
                    if (tx.pmtId.endToEndId != null)
                        addText(doc, pmtId, "EndToEndId", tx.pmtId.endToEndId);
                    if (tx.pmtId.txId != null)
                        addText(doc, pmtId, "TxId", tx.pmtId.txId);
                    if (tx.pmtId.uetr != null)
                        addText(doc, pmtId, "UETR", tx.pmtId.uetr);
                }

                Element amt = doc.createElementNS(NS, "IntrBkSttlmAmt");
                amt.setAttribute("Ccy", tx.intrBkSttlmCcy);
                amt.setTextContent(tx.intrBkSttlmAmt.toPlainString());
                cdtTrfTxInf.appendChild(amt);

                addText(doc, cdtTrfTxInf, "IntrBkSttlmDt", tx.intrBkSttlmDt.toString());

                if (tx.debtor != null) {
                    Element dbtr = doc.createElementNS(NS, "Dbtr");
                    cdtTrfTxInf.appendChild(dbtr);
                    addText(doc, dbtr, "Nm", tx.debtor.name);
                }
                if (tx.creditor != null) {
                    Element cdtr = doc.createElementNS(NS, "Cdtr");
                    cdtTrfTxInf.appendChild(cdtr);
                    addText(doc, cdtr, "Nm", tx.creditor.name);
                }
                if (tx.remittanceInfoUnstructured != null) {
                    Element rmtInf = doc.createElementNS(NS, "RmtInf");
                    cdtTrfTxInf.appendChild(rmtInf);
                    addText(doc, rmtInf, "Ustrd", tx.remittanceInfoUnstructured);
                }
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter sw = new StringWriter();
            t.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        }

        private void addText(Document doc, Element parent, String name, String text) {
            Element el = doc.createElementNS(NS, name);
            el.setTextContent(text);
            parent.appendChild(el);
        }

        private void addBicAgent(Document doc, Element parent, Pacs008.Agent ag) {
            Element finInstnId = doc.createElementNS(NS, "FinInstnId");
            parent.appendChild(finInstnId);
            addText(doc, finInstnId, "BICFI", ag.finInstn.finInstnId.bic);
        }
    }

    // ---------- Main demo ----------

    public static void main(String[] args) throws Exception {
        String fin =
                "{1:F01BANKBEBBAXXX2222123456}" +
                "{2:O1031201050103BANKDEFFXXXX22221234560501031201N}" +
                "{3:{121:123e4567-e89b-12d3-a456-426614174000}{108:ABC123}}" +
                "{4:\n" +
                ":20:REF12345\n" +
                ":23B:CRED\n" +
                ":32A:250606EUR12345,67\n" +
                ":50K:/123456789\n" +
                "JOHN DOE\n" +
                "1 MAIN STREET\n" +
                "BRUSSELS\n" +
                ":59:/987654321\n" +
                "JUAN PEREZ\n" +
                "AVENIDA REFORMA\n" +
                "MEXICO CITY\n" +
                ":70:INVOICE 987\n" +
                ":71A:SHA\n" +
                "-}";

        MtParser parser = new MtParser();
        Mt103Envelope env = parser.parse(fin);

        MtToPacsMapper mapper = new MtToPacsMapper();
        Pacs008 mx = mapper.map(env);

        Pacs008XmlBuilder builder = new Pacs008XmlBuilder();
        String xml = builder.toXml(mx);

        System.out.println(xml);
    }
}
