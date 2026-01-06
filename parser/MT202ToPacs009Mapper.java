package com.karan.swifttranslator.custom.parser;

// File: MtToPacs008Mapper.java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class MT202ToPacs009Mapper {

	/* ======== Config model ======== */

	public static class MappingConfig {
		public List<MappingRule> mappings;
	}

	public static class MappingRule {
		public String mtKey; // e.g. "b4.32A.amount"
		public String mxPath; // e.g. "CdtTrfTxInf.IntrBkSttlmAmt@Ccy"
		public String transform; // e.g. write custom logic
		public String value; // in case of mtKey constant
		public String format; // e.g. regex or date format
		public String action; // e.g. Action if any like trim
	}

	/* ======== Mapper ======== */

	public static class JsonDrivenMapper {
		private final MappingConfig config;

		public JsonDrivenMapper(MappingConfig config) {
			this.config = config;
		}

		public Document map(Mt202ConfigDrivenParser.MtMessage mt) throws Exception {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();

			String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.009.001.08";
			Element root = doc.createElementNS(NS, "Document");
			doc.appendChild(root);
			Element fiToFi = doc.createElementNS(NS, "FICdtTrf");
			root.appendChild(fiToFi);

			for (MappingRule r : config.mappings) {
				String value;

				if (r.mtKey != null && r.mtKey.startsWith("CONSTANT")) {
					value = r.value;
				} else {
					value = mt.fields.get(r.mtKey);
				}

				if (value == null || value.isEmpty())
					continue;

				if (r.transform != null) {
					value = applyTransform(r.transform, value, r);
					if (value == null || value.isEmpty()) {
						continue;
					}
				}

				applyPath(doc, fiToFi, r.mxPath, value);
			}

			return doc;
		}

		private String applyTransform(String name, String value, MappingRule rule) {
			switch (name) {
			case "mapChargeBearer":
				return getChargeBearer(value);
			case "numericNormalize":
				return value.replace(",", ".");
			case "formatYYMMDDtoYYYYMMDD":
				if (value.length() == 6) {
					String yy = value.substring(0, 2);
					String mm = value.substring(2, 4);
					String dd = value.substring(4, 6);
					String yyyy = "20" + yy; // or smarter century logic if you want
					return yyyy + "-" + mm + "-" + dd;
				}
				return value;
			case "joinLines":
				// for mtKey that is a list, you can adjust your parser to store joined value
				return value.trim();
			default:
				return value;
			}
		}

		/**
		 * @param value
		 * @return
		 */
		private String getChargeBearer(String value) {
			switch (value) {
			case "OUR":
				value = "DEBT";
				break;
			case "BEN":
				value = "CRED";
				break;
			case "SHA":
				value = "SHAR";
				break;
			default:
				;
				break;
			}
			return value;
		}

		private void applyPath(Document doc, Element parent, String path, String value) {
			// Support @attribute and simple [index] arrays
			String[] parts = path.split("\\.");

			Element current = parent;
			for (int i = 0; i < parts.length; i++) {
				String part = parts[i];

				// attribute on current element
				if (part.contains("@")) {
					String[] pe = part.split("@", 2);
					String elemName = pe[0];
					String attrName = pe[1];

					if (!elemName.isEmpty()) {
						current = ensureChild(doc, current, elemName, 0);
					}
					current.setAttribute(attrName, value);
					return;
				}

				// list index: Name[0]
				int idx = 0;
				String elemName = part;
				int bracket = part.indexOf('[');
				if (bracket >= 0 && part.endsWith("]")) {
					elemName = part.substring(0, bracket);
					String sIdx = part.substring(bracket + 1, part.length() - 1);
					idx = Integer.parseInt(sIdx);
				}

				// last segment -> set text
				if (i == parts.length - 1) {
					Element child = ensureChild(doc, current, elemName, idx);
					child.setTextContent(value);
				} else {
					current = ensureChild(doc, current, elemName, idx);
				}
			}
		}

		private Element ensureChild(Document doc, Element parent, String name, int index) {
			NodeList existing = parent.getElementsByTagNameNS(parent.getNamespaceURI(), name);
			// Filter only direct children of this parent
			List<Element> direct = new ArrayList<>();
			for (int i = 0; i < existing.getLength(); i++) {
				Node n = existing.item(i);
				if (n.getParentNode() == parent) {
					direct.add((Element) n);
				}
			}
			while (direct.size() <= index) {
				Element e = doc.createElementNS(parent.getNamespaceURI(), name);
				parent.appendChild(e);
				direct.add(e);
			}
			return direct.get(index);
		}
	}

	/* ======== Utility: XML to String ======== */

	public static String toXmlString(Document doc) throws Exception {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		StringWriter sw = new StringWriter();
		t.transform(new DOMSource(doc), new StreamResult(sw));
		return sw.toString();
	}

	/* ======== Demo main wiring parser + mapper ======== */

	public static void main(String[] args) throws Exception {
		ObjectMapper om = new ObjectMapper();

		// 1) Load MT103 schema and parse MT
		Mt202ConfigDrivenParser.MtSchema mtSchema;
		try (InputStream in = Files.newInputStream(Paths
				.get("D:\\JavaPractice\\pacs008-mt103-swifttranslator\\src\\main\\resources\\mt103-schema-new.json"))) {
			mtSchema = om.readValue(in, Mt202ConfigDrivenParser.MtSchema.class);
		}
		Mt202ConfigDrivenParser.MtParser mtParser = new Mt202ConfigDrivenParser.MtParser(mtSchema);

		String mt103 = "{1:F01BANKMAMCXXXX1111111111}{2:I103BKAMMAMAXXXXN}{3:{113:0020}{111:001}{121:b5b2b068-4cef-4b78-a654-bfc5d31406c5}}{4:\r\n"
				+ ":20:MAIRMCL250010\r\n" + ":23B:CRED\r\n" + ":23E:SDVA\r\n" + ":32A:251205MAD243,27\r\n"
				+ ":50K:/028780000000010050502963\r\n" + "CABN MAASD SDFGT\r\n" + "14, AAG 9 N18, AA UJKAGSE\r\n"
				+ "CASABLABNA\r\n" + "CASABKJHA, MA\r\n" + ":52A:BANKMAMC\r\n" + ":53A:/D/0028\r\n" + "BANKMAMC\r\n"
				+ ":57A:/C/0023\r\n" + "SGMBMAMCFCM\r\n" + ":59:/363780000001000002076281\r\n" + "UKHACHKU UJKLHGA\r\n"
				+ "DKK AZ NMSHAK\r\n" + ":70:.\r\n" + ":71A:SHA\r\n" + ":72:/CODTYPTR/001\r\n"
				+ "//CUSTOMER DEMAND DEPOSITS NIB\r\n" + "-}";

		Mt202ConfigDrivenParser.MtMessage mt = mtParser.parse(mt103);

		// 2) Load MT→pacs.008 mapping config
		MappingConfig cfg;
		try (InputStream in = Files.newInputStream(Paths
				.get("D:\\JavaPractice\\pacs008-mt103-swifttranslator\\src\\main\\resources\\mt103-to-pacs008.json"))) {
			cfg = om.readValue(in, MappingConfig.class);
		}

		// 3) Map to pacs.008 XML
		JsonDrivenMapper mapper = new JsonDrivenMapper(cfg);
		Document doc = mapper.map(mt);

		// 4) Print XML
		System.out.println(toXmlString(doc));

		for (int i = 0; i < 2; i++) {
			long startTime = System.nanoTime(); // Get the current time in nanoseconds
			Mt202ConfigDrivenParser.MtMessage mt1 = mtParser.parse(mt103);
			long parsingTime = System.nanoTime(); // Get the current time in nanoseconds
			JsonDrivenMapper mapper1 = new JsonDrivenMapper(cfg);
			Document doc1 = mapper1.map(mt1);
			System.out.println(toXmlString(doc1));
			long endTime = System.nanoTime(); // Get the current time again after method execution

			long durationInNanoseconds = endTime - startTime;
			double durationInMilliseconds = (double) durationInNanoseconds / 1_000_000.0;

//			System.out.println("Method execution time: " + durationInNanoseconds + " nanoseconds");
			System.out.println("Method execution time: " + durationInMilliseconds + " milliseconds, Parsing time : "
					+ (endTime - parsingTime) / 1_000_000.0);

		}

	}
}
