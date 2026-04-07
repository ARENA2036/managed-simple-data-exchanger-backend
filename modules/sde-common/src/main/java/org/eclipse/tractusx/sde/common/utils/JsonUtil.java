package org.eclipse.tractusx.sde.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static boolean isJsonObjectString(Object obj) {
        if (!(obj instanceof String s)) {
            return false;
        }

        s = s.trim();
        if (s.isEmpty()){
            return false;
        }

        // cheap pre-check (optional)
        if (s.charAt(0) != '{' || s.charAt(s.length() - 1) != '}') {
            return false;
        }

        try {
            JsonNode node = MAPPER.readTree(s);
            return node != null && node.isObject();
        } catch (Exception e) {
            return false; // not valid JSON object
        }
    }
}
