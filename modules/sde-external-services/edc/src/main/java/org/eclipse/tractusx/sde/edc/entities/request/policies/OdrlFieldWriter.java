package org.eclipse.tractusx.sde.edc.entities.request.policies;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.experimental.UtilityClass;

import java.io.IOException;

@UtilityClass
public class OdrlFieldWriter {
    public static final String ODRL_PREFIX = "odrl:";
    public static final String EDC_PREFIX = "odrl:";

    public static void writeOdrlField(JsonGenerator gen, SerializerProvider provider, boolean withPrefix, String baseName, Object fieldValue)
            throws IOException {
        if (fieldValue == null) return;
        String prefix = withPrefix ? ODRL_PREFIX: "";
        write(gen,provider,prefix,baseName,fieldValue);
    }

    public static void writeEdcField(JsonGenerator gen, SerializerProvider provider, boolean withPrefix, String baseName, Object fieldValue)
            throws IOException {
        if (fieldValue == null) return;
        String prefix = withPrefix ? EDC_PREFIX: "";
        write(gen,provider,prefix,baseName,fieldValue);
    }

    public static void write(
            JsonGenerator gen,
            SerializerProvider provider,
            String prefix,
            String baseName,
            Object fieldValue)
            throws IOException {
        if (fieldValue == null) return;
        gen.writeFieldName(prefix + baseName);
        provider.defaultSerializeValue(fieldValue, gen);
    }
}
