package com.karan.swifttranslator.custom.parser;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Working PARSER

// File: Mt103ConfigDrivenParser.java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Mt103ConfigDrivenParser {

    /* ======================== DOMAIN OUTPUT ======================== */

    public static class MtMessage {
        public String raw;
        public String block1;
        public String block2;
        public String block3;
        public String block4;
        public String block5;
        public Map<String, String> fields = new LinkedHashMap<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("MT Message fields:\n");
            for (Map.Entry<String, String> e : fields.entrySet()) {
                sb.append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
            }
            return sb.toString();
        }
    }

    /* ======================== SCHEMA MODEL ======================== */

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MtSchema {
        public String messageType;
        public List<Integer> blocks;
        public List<FieldDef> fields;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldDef {
        public String tag;          // e.g. "20", "32A", "50K", "B1", "B2"
        public String option;       // e.g. "A","F","K" (for 50x etc.), may be null
        public int block;           // 1..5
        public String name;
        public boolean mandatory;
        public int maxOccurs;
        public FormatDef format;
        public List<ComponentDef> components;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FormatDef {
        public String type;         // simple, dateCurrencyAmount, currencyAmount, partyLines, block1, block2, block3Tag, etc.
        public Integer maxLines;
        public Integer maxLength;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ComponentDef {
        public String name;         // logical name
        public String mtKey;        // e.g. "b4.20", "b4.32A.amount", "b1.senderBic11"
    }

    /* ======================== PARSER ENGINE ======================== */

    public static class MtParser {
        private final MtSchema schema;
        private final Map<Integer, List<FieldDef>> blockFields = new HashMap<>();
        private final Map<String, List<FieldDef>> block3FieldsByTag = new HashMap<>();
//        private final ObjectMapper mapper = new ObjectMapper();

        // regex for block 4 fields like ":20:VALUE"
        private static final Pattern TAG_LINE = Pattern.compile("^:(\\d{2,3}[A-Z]?):(.*)$");

        public MtParser(MtSchema schema) {
            this.schema = schema;
            indexSchema();
        }

        private void indexSchema() {
            if (schema.fields == null) return;
            for (FieldDef f : schema.fields) {
                blockFields.computeIfAbsent(f.block, b -> new ArrayList<>()).add(f);
                if (f.block == 3 && f.tag.matches("\\d+")) {
                    block3FieldsByTag.computeIfAbsent(f.tag, t -> new ArrayList<>()).add(f);
                }
            }
        }

        public MtMessage parse(String fin) {
            MtMessage mt = new MtMessage();
            mt.raw = fin;

            // 1) split blocks {1:}{2:}{3:}{4:}{5:}
            mt.block1 = extractBlock(fin, "1");
            mt.block2 = extractBlock(fin, "2");
            mt.block3 = extractBlock(fin, "3");
            mt.block4 = extractBlock(fin, "4");
            mt.block5 = extractBlock(fin, "5");

            // 2) parse header blocks using schema
            parseBlock1(mt);
            parseBlock2(mt);
            parseBlock3(mt);

            // 3) parse block 4 fields generically
            parseBlock4(mt);

            return mt;
        }

        private String extractBlock(String fin, String blockId) {
            String start = "{" + blockId + ":";
            int s = fin.indexOf(start);
            if (s < 0) return null;
            int depth = 0;
            for (int i = s; i < fin.length(); i++) {
                char c = fin.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        // inclusive of trailing }
                        return fin.substring(s, i + 1);
                    }
                }
            }
            return null;
        }

        /* ---------- Block 1: use format.type = block1 ---------- */

        private void parseBlock1(MtMessage mt) {
            if (mt.block1 == null) return;
            List<FieldDef> defs = blockFields.getOrDefault(1, Collections.emptyList());
            FieldDef b1def = defs.stream()
                    .filter(d -> "B1".equals(d.tag) && d.format != null && "block1".equals(d.format.type))
                    .findFirst().orElse(null);
            if (b1def == null) return;

            // content inside {1: ... }
            String content = mt.block1.substring(3, mt.block1.length() - 1);
            if (content.length() < 20) return;

            String appId = content.substring(0, 1);
            String serviceId = content.substring(1, 3);
            String ltAddress = content.substring(3, 15);
            String session = content.substring(15, 19);
            String sequence = content.substring(19, 25 > content.length() ? content.length() : 25);

            putComponent(mt, b1def, "appId", appId);
            putComponent(mt, b1def, "serviceId", serviceId);
            putComponent(mt, b1def, "ltAddress", ltAddress);
            // derive BIC11 from LT address
            String bic11 = ltAddress.substring(0, Math.min(11, ltAddress.length()));
            putComponent(mt, b1def, "senderBic11", bic11);
            String bic8 = ltAddress.substring(0, Math.min(8, ltAddress.length()));
            putComponent(mt, b1def, "senderBic8", bic8);
            putComponent(mt, b1def, "sessionNumber", session);
            putComponent(mt, b1def, "sequenceNumber", sequence);
        }

        /* ---------- Block 2: use format.type = block2 ---------- */

        private void parseBlock2(MtMessage mt) {
            if (mt.block2 == null) return;
            List<FieldDef> defs = blockFields.getOrDefault(2, Collections.emptyList());
            FieldDef b2def = defs.stream()
                    .filter(d -> "B2".equals(d.tag) && d.format != null && "block2".equals(d.format.type))
                    .findFirst().orElse(null);
            if (b2def == null) return;

            String content = mt.block2.substring(3, mt.block2.length() - 1);
            if (content.isEmpty()) return;

            char direction = content.charAt(0); // I or O
            String dir = String.valueOf(direction);
            String msgType = content.length() >= 4 ? content.substring(1, 4) : null;
            String receiverBic = null;
            String priority = null;
            String inputTime = null;
            String mir = null;

            if (direction == 'I') {
                if (content.length() >= 16) {
                    receiverBic = content.substring(4, 16);
                }
                if (content.length() >= 17) {
                    priority = content.substring(16, 17);
                }
            } else if (direction == 'O') {
                if (content.length() >= 16) {
                    receiverBic = content.substring(7, 19);
                }
                if (content.length() >= 20) {
                    priority = content.substring(19, 20);
                }
                if (content.length() >= 7) {
                    inputTime = content.substring(4, 8);
                }
                if (content.length() >= 29) {
                    mir = content.substring(8, 29);
                }
            }

            putComponent(mt, b2def, "direction", dir);
            putComponent(mt, b2def, "msgType", msgType);
            putComponent(mt, b2def, "receiverBic", receiverBic);
            putComponent(mt, b2def, "priority", priority);
            putComponent(mt, b2def, "inputTime", inputTime);
            putComponent(mt, b2def, "mir", mir);
        }

        /* ---------- Block 3: use format.type = block3Tag ---------- */

        private void parseBlock3(MtMessage mt) {
            if (mt.block3 == null) return;
            // strip "{3:" and trailing "}"
            String content = mt.block3.substring(3, mt.block3.length() - 1);
            // inside is like "{108:...}{121:...}{119:...}"
            int i = 0;
            while (i < content.length()) {
                int open = content.indexOf('{', i);
                if (open < 0) break;
                int colon = content.indexOf(':', open + 1);
                int close = content.indexOf("}", colon + 1);
                if (colon < 0 || close < 0) break;
                String tag = content.substring(open + 1, colon);
                String value = content.substring(colon + 1, close);
                List<FieldDef> defs = block3FieldsByTag.get(tag);
                if (defs != null) {
                    for (FieldDef d : defs) {
                        if (d.format != null && "block3Tag".equals(d.format.type)) {
                            // simple one-component mapping
                            if (d.components != null && !d.components.isEmpty()) {
                                ComponentDef c = d.components.get(0);
                                mt.fields.put(c.mtKey, value);
                            }
                        }
                    }
                }
                i = close + 1;
            }
        }

        /* ---------- Block 4: tag-based generic parsing ---------- */

        private void parseBlock4(MtMessage mt) {
            if (mt.block4 == null) return;
            // strip {4: and -}
            String body = mt.block4;
            int start = body.indexOf("{4:");
            if (start >= 0) {
                body = body.substring(start + 3);
            }
            int end = body.lastIndexOf("-}");
            if (end >= 0) {
                body = body.substring(0, end);
            }

            String[] lines = body.split("\\r?\\n");
            String currentTag = null;
            StringBuilder currentValue = new StringBuilder();

            List<String[]> fieldsLines = new ArrayList<>();

            for (String line : lines) {
                Matcher m = TAG_LINE.matcher(line);
                if (m.matches()) {
                    // flush previous
                    if (currentTag != null) {
                        fieldsLines.add(new String[]{currentTag, currentValue.toString()});
                    }
                    currentTag = m.group(1);           // e.g. "20", "32A", "50K"
                    currentValue.setLength(0);
                    currentValue.append(m.group(2));   // rest of line
                } else {
                    // continuation line
                    if (currentTag != null) {
                        currentValue.append("\n").append(line);
                    }
                }
            }
            if (currentTag != null) {
                fieldsLines.add(new String[]{currentTag, currentValue.toString()});
            }

            // Apply schema to each tag instance
            for (String[] tv : fieldsLines) {
                String tag = tv[0];
                String value = tv[1];
                List<FieldDef> defs = blockFields.getOrDefault(4, Collections.emptyList());
                for (FieldDef d : defs) {
                    if (!d.tag.equals(tag)) continue;
                    applyFieldFormat(mt, d, value);
                }
            }
        }

        /* ---------- Format handlers ---------- */

        private void applyFieldFormat(MtMessage mt, FieldDef def, String raw) {
            if (def.format == null || def.format.type == null) return;
            String type = def.format.type;

            switch (type) {
                case "simple":
                    applySimple(mt, def, raw);
                    break;
                case "dateCurrencyAmount":
                    applyDateCurrencyAmount(mt, def, raw);
                    break;
                case "currencyAmount":
                    applyCurrencyAmount(mt, def, raw);
                    break;
                case "rate":
                    applyRate(mt, def, raw);
                    break;
                case "partyLines":
                    applyPartyLines(mt, def, raw);
                    break;
                case "structuredParty":
                    applyPartyLines(mt, def, raw);
                    break;
                case "bicParty":
                    applyBicParty(mt, def, raw);
                    break;
                case "accountParty":
                    applyAccountParty(mt, def, raw);
                    break;
                case "freeTextLines":
                    applyFreeTextLines(mt, def, raw);
                    break;
                case "regulatoryLines":
                    applyFreeTextLines(mt, def, raw);
                    break;
                default:
                    // not implemented format type; ignore
                    break;
            }
        }

        private void applySimple(MtMessage mt, FieldDef def, String raw) {
            if (def.components == null || def.components.isEmpty()) return;
            // trim and clip
            String v = raw.trim();
            if (def.format.maxLength != null && v.length() > def.format.maxLength) {
                v = v.substring(0, def.format.maxLength);
            }
            ComponentDef c = def.components.get(0);
            mt.fields.put(c.mtKey, v);
        }

        private void applyDateCurrencyAmount(MtMessage mt, FieldDef def, String raw) {
            // YYMMDDCCCAMOUNT with comma as decimal
            String v = raw.replace("\n", "").trim();
            if (v.length() < 10) return;
            String date = v.substring(0, 6);
            String ccy = v.substring(6, 9);
            String amountStr = v.substring(9).replace(",", ".");
            for (ComponentDef c : def.components) {
                if ("date".equals(c.name)) {
                    mt.fields.put(c.mtKey, date);
                } else if ("ccy".equals(c.name)) {
                    mt.fields.put(c.mtKey, ccy);
                } else if ("amount".equals(c.name)) {
                    mt.fields.put(c.mtKey, amountStr);
                }
            }
        }

        private void applyCurrencyAmount(MtMessage mt, FieldDef def, String raw) {
            String v = raw.replace("\n", "").trim();
            if (v.length() < 4) return;
            String ccy = v.substring(0, 3);
            String amountStr = v.substring(3).replace(",", ".");
            for (ComponentDef c : def.components) {
                if ("ccy".equals(c.name)) {
                    mt.fields.put(c.mtKey, ccy);
                } else if ("amount".equals(c.name)) {
                    mt.fields.put(c.mtKey, amountStr);
                }
            }
        }

        private void applyRate(MtMessage mt, FieldDef def, String raw) {
            String rate = raw.replace("\n", "").trim();
            for (ComponentDef c : def.components) {
                if ("rate".equals(c.name)) {
                    mt.fields.put(c.mtKey, rate);
                }
            }
        }

        private void applyPartyLines(MtMessage mt, FieldDef def, String raw) {
            String[] lines = raw.split("\\r?\\n");
            for (ComponentDef c : def.components) {
                // expect component names like account/name/address0/address1/...
                if ("account".equals(c.name)) {
                    if (lines.length > 0 && lines[0].startsWith("/")) {
                        mt.fields.put(c.mtKey, lines[0].substring(1));
                    }
                } else if (c.name.startsWith("address") || "name".equals(c.name)) {
                    int idx;
                    if ("name".equals(c.name)) {
                        // name = first non-account line
                        idx = (lines.length > 0 && lines[0].startsWith("/")) ? 1 : 0;
                        if (idx < lines.length) {
                            mt.fields.put(c.mtKey, lines[idx]);
                        }
                    } else {
                        // addressN -> subsequent lines
                        idx = Character.getNumericValue(c.name.charAt(c.name.length() - 1));
                        // address0 means first address line after name (rough logic)
                        int base = (lines.length > 0 && lines[0].startsWith("/")) ? 1 : 0;
                        int lineIdx = base + 1 + idx; // simplistic, adjust if needed
                        if (lineIdx < lines.length) {
                            mt.fields.put(c.mtKey, lines[lineIdx]);
                        }
                    }
                }
            }
        }

        private void applyBicParty(MtMessage mt, FieldDef def, String raw) {
            // typical format: "/ACCOUNT\nBIC" or just "BIC"
            String[] lines = raw.split("\\r?\\n");
            String account = null;
            String bic = null;
            if (lines.length == 1) {
                bic = lines[0].trim();
            } else if (lines.length >= 2) {
                if (lines[0].startsWith("/")) {
                    account = lines[0].substring(1).trim();
                    bic = lines[1].trim();
                } else {
                    bic = lines[0].trim();
                }
            }
            for (ComponentDef c : def.components) {
                if ("account".equals(c.name) && account != null) {
                    mt.fields.put(c.mtKey, account);
                } else if ("bic".equals(c.name) && bic != null) {
                    mt.fields.put(c.mtKey, bic);
                }
            }
        }

        private void applyAccountParty(MtMessage mt, FieldDef def, String raw) {
            String[] lines = raw.split("\\r?\\n");
            String account = null;
            String name = null;
            if (lines.length > 0) {
                if (lines[0].startsWith("/")) {
                    account = lines[0].substring(1).trim();
                    if (lines.length > 1) name = lines[1].trim();
                } else {
                    name = lines[0].trim();
                }
            }
            for (ComponentDef c : def.components) {
                if ("account".equals(c.name) && account != null) {
                    mt.fields.put(c.mtKey, account);
                } else if ("name".equals(c.name) && name != null) {
                    mt.fields.put(c.mtKey, name);
                }
            }
        }

        private void applyFreeTextLines(MtMessage mt, FieldDef def, String raw) {
            String[] lines = raw.split("\\r?\\n");
            for (ComponentDef c : def.components) {
                if (c.name.startsWith("line")) {
                    int idx = Character.getNumericValue(c.name.charAt(c.name.length() - 1));
                    if (idx < lines.length) {
                        mt.fields.put(c.mtKey, lines[idx]);
                    }
                }
            }
        }

        private void putComponent(MtMessage mt, FieldDef def, String compName, String value) {
            if (value == null) return;
            if (def.components == null) return;
            for (ComponentDef c : def.components) {
                if (compName.equals(c.name)) {
                    mt.fields.put(c.mtKey, value);
                }
            }
        }
    }

    /* ======================== MAIN DEMO ======================== */

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // 1) Load schema (adjust path as needed)
        //   Option A: from file system
        Path schemaPath = Paths.get("D:\\JavaPractice\\pacs008-mt103-swifttranslator\\src\\main\\resources\\mt103-schema-new.json");
        MtSchema schema;
        try (InputStream in = Files.newInputStream(schemaPath)) {
            schema = mapper.readValue(in, MtSchema.class);
        }

        // 2) Build parser
        MtParser parser = new MtParser(schema);

        // 3) Sample MT103 (anonymised; replace with your real message)
        String mt103 =
                "{1:F01AAAAUSC0ADDD0344000050}{2:I103BBBBUSC0XFFFN}{3:{121:123e4567-e89b-12d3-a456-426614174000}}" +
                "{4:\n" +
                ":20:REF123456789\n" +
                ":23B:CRED\n" +
                ":32A:250201USD1234,56\n" +
                ":50K:/1234567890\n" +
                "JOHN DOE\n" +
                "1 MAIN STREET\n" +
                "NEW YORK\n" +
                ":59:/9988776655\n" +
                "ACME CORP\n" +
                "2 HIGH STREET\n" +
                "LONDON\n" +
                ":70:INVOICE 987\n" +
                ":71A:OUR\n" +
                ":72:/INS/INSTRUCTION TEXT\n" +
                "-}";

        mt103="{1:F01BANKMAMCXXXX1111111111}{2:I103BKAMMAMAXXXXN}{3:{113:0020}{111:001}{121:b5b2b068-4cef-4b78-a654-bfc5d31406c5}}{4:\r\n"
        		+ ":20:MAIRMCL250010\r\n"
        		+ ":23B:CRED\r\n"
        		+ ":23E:SDVA\r\n"
        		+ ":32A:251205MAD243,27\r\n"
        		+ ":50K:/028780000000010050502963\r\n"
        		+ "CABN MAASD SDFGT\r\n"
        		+ "14, AAG 9 N18, AA UJKAGSE\r\n"
        		+ "CASABLABNA\r\n"
        		+ "CASABKJHA, MA\r\n"
        		+ ":52A:BANKMAMC\r\n"
        		+ ":53A:/D/0028\r\n"
        		+ "BANKMAMC\r\n"
        		+ ":57A:/C/0023\r\n"
        		+ "SGMBMAMCFCM\r\n"
        		+ ":59:/363780000001000002076281\r\n"
        		+ "UKHACHKU UJKLHGA\r\n"
        		+ "DKK AZ NMSHAK\r\n"
        		+ ":70:.\r\n"
        		+ ":71A:SHA\r\n"
        		+ ":72:/CODTYPTR/001\r\n"
        		+ "//CUSTOMER DEMAND DEPOSITS NIB\r\n"
        		+ "-}";
        // 4) Parse
        MtMessage mt = parser.parse(mt103);

        // 5) Print MT object
        System.out.println(mt);
    }
}
