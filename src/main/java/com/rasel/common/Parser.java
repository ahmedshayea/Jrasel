package com.rasel.common;

import java.util.HashMap;

/**
 * General parser that performs initial parsing to data
 */
public class Parser {

    final static HashMap<String, String> macros = new HashMap<String, String>();

    public Parser(String stream) {
        initialParse(stream);
    }

    static void initialParse(String stream) {
        String[] lines = stream.split("\n");
        for (String line : lines) {
            String[] dict = line.split(":", 2);
            if (dict.length >= 1) {
                String key = dict[0].toUpperCase();
                // check if keyword exist inside default macros
                if (!macros.containsKey(key) || dict.length != 2) {
                    continue;
                }
                macros.put(key, dict[1]);
            }
        }
    }
}
