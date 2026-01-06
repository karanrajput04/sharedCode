package com.karan.swifttranslator.custom.parser;

import java.util.List;

public class ValidationRule {
    public String type;
    public String mtKey;
    public String message;

    public Integer min;
    public Integer max;

    public String pattern;
    public List<String> values;

    public String whenMtKey;
    public Boolean whenPresent;

    public List<String> mtKeys;   // for mutuallyExclusive / atMostOneOf

    public String operator;       // for compare: ">", "<", ">=", "<=", "=="
    public String threshold;      // string, parsed as number when needed
}

