package org.eclipse.tractusx.sde.edc.entities.request.policies;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

import static org.eclipse.tractusx.sde.edc.entities.request.policies.OdrlFieldWriter.writeEdcField;
import static org.eclipse.tractusx.sde.edc.entities.request.policies.OdrlFieldWriter.writeOdrlField;

public class PolicyDefinitionRequestSerializer extends StdSerializer<PolicyDefinitionRequest> {

    public PolicyDefinitionRequestSerializer() {
        super(PolicyDefinitionRequest.class);
    }

    @Override
    public void serialize(PolicyDefinitionRequest value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        boolean useNameSpacePrefix = value.isUseNameSpacePrefix();

        value.getPolicy().setUseNameSpacePrefix(useNameSpacePrefix);


        gen.writeStartObject();

        // fields without prefix
        if (value.getContext() != null) {
            gen.writeFieldName("@context");
            provider.defaultSerializeValue(value.getContext(), gen);
        }
        if (value.getId() != null) {
            gen.writeStringField("@id", value.getId());
        }

        if (value.getId() != null) {
            gen.writeStringField("@type", value.getPolityRootType());
        }

        // conditional prefix fields
        writeEdcField(gen, provider, useNameSpacePrefix, "policy", value.getPolicy());
        if (value.getProfile() != null) {
            writeOdrlField(gen, provider, useNameSpacePrefix, "profile", value.getProfile());
        }

        gen.writeEndObject();
    }

}