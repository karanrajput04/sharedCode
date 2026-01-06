package com.karan.swifttranslator.custom.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.karan.swifttranslator.custom.parser.Mt103ConfigDrivenParser.MtMessage;

public class MtValidator {

	private final ValidationConfig cfg;

	public MtValidator(ValidationConfig cfg) {
		this.cfg = cfg;
	}

	public List<String> validate(MtMessage mt) {
		List<String> errors = new ArrayList<>();
		Map<String, String> f = mt.fields;

		for (ValidationRule r : cfg.rules) {
			switch (r.type) {
			case "required":
				if (isEmpty(f.get(r.mtKey))) {
					errors.add(msg(r, "Required field missing: " + r.mtKey));
				}
				break;

			case "length":
				String v = f.get(r.mtKey);
				if (!isEmpty(v)) {
					int len = v.length();
					if ((r.min != null && len > r.min) || (r.max != null && len < r.max)) {
						errors.add(msg(r, "Length violation on " + r.mtKey + ", expected : [" + r.min + "," + r.max
								+ "], Found : " + len + " (" + v + ")"));
					}
				}
				break;

			case "regex":
				v = f.get(r.mtKey);
				if (!isEmpty(v) && !v.matches(r.pattern)) {
					errors.add(msg(r, "Format violation on " + r.mtKey));
				}
				break;

			case "allowedValues":
				v = f.get(r.mtKey);
				if (!isEmpty(v) && (r.values == null || !r.values.contains(v))) {
					errors.add(msg(r, "Invalid value for " + r.mtKey + ": " + v));
				}
				break;

			case "forbiddenValues":
				v = f.get(r.mtKey);
				if (!isEmpty(v) && r.values != null && r.values.contains(v)) {
					errors.add(msg(r, "Forbidden value for " + r.mtKey + ": " + v));
				}
				break;

			case "conditionalRequired":
				boolean cond = !isEmpty(f.get(r.whenMtKey));
				if (Boolean.FALSE.equals(r.whenPresent)) {
					cond = !cond;
				}
				if (cond && isEmpty(f.get(r.mtKey))) {
					errors.add(msg(r, "Conditional required field missing: " + r.mtKey));
				}
				break;

			case "mutuallyExclusive":
				handleExclusive(r, f, errors, true);
				break;

			case "atMostOneOf":
				handleExclusive(r, f, errors, false);
				break;

			case "compare":
				v = f.get(r.mtKey);
				if (!isEmpty(v) && !compareNumeric(v, r.operator, r.threshold)) {
					errors.add(msg(r, "Comparison failed on " + r.mtKey));
				}
				break;

			default:
				// ignore unknown rule types
			}
		}
		return errors;
	}

	private boolean isEmpty(String s) {
		return s == null || s.isEmpty();
	}

	private String msg(ValidationRule r, String fallback) {
		return (r.message != null && !r.message.isEmpty()) ? r.message : fallback;
	}

	private void handleExclusive(ValidationRule r, Map<String, String> f, List<String> errors, boolean exactlyOne) {
		if (r.mtKeys == null || r.mtKeys.isEmpty())
			return;
		int count = 0;
		for (String k : r.mtKeys) {
			if (!isEmpty(f.get(k)))
				count++;
		}
		if (exactlyOne) {
			if (count != 1) {
				errors.add(msg(r, "Exactly one of " + r.mtKeys + " must be present"));
			}
		} else {
			if (count > 1) {
				errors.add(msg(r, "At most one of " + r.mtKeys + " may be present"));
			}
		}
	}

	private boolean compareNumeric(String v, String op, String thr) {
		try {
			java.math.BigDecimal val = new java.math.BigDecimal(v.replace(",", "."));
			java.math.BigDecimal t = new java.math.BigDecimal(thr.replace(",", "."));
			int cmp = val.compareTo(t);
			switch (op) {
			case ">":
				return cmp > 0;
			case "<":
				return cmp < 0;
			case ">=":
				return cmp >= 0;
			case "<=":
				return cmp <= 0;
			case "==":
				return cmp == 0;
			default:
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}
}
