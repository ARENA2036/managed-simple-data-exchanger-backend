package org.eclipse.tractusx.sde.edc.entities.request.policies;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import static org.eclipse.tractusx.sde.edc.entities.request.policies.OdrlFieldWriter.writeOdrlField;

public class PolicyRequestSerializer extends StdSerializer<PolicyRequest> {

    public PolicyRequestSerializer() {
        super(PolicyRequest.class);
    }

    @Override
    public void serialize(PolicyRequest value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        boolean useNameSpacePrefix = value.isUseNameSpacePrefix();

        applyUseNamespaceFlagToPermissions(value, useNameSpacePrefix);

        gen.writeStartObject();

        // fields without prefix
        if (value.getType() != null) {
            gen.writeStringField("@type", value.getType());
        }
        if (value.getContext() != null) {
            gen.writeFieldName("@context");
            provider.defaultSerializeValue(value.getContext(), gen);
        }
        if (value.getId() != null) {
            gen.writeStringField("@id", value.getId());
        }

        // conditional odrl fields
        writeOdrlField(gen, provider, useNameSpacePrefix, "permission", value.getPermission());
        writeOdrlField(gen, provider, useNameSpacePrefix, "prohibition", value.getProhibition());
        writeOdrlField(gen, provider, useNameSpacePrefix, "obligation", value.getObligation());
        writeOdrlField(gen, provider, useNameSpacePrefix, "target", value.getTarget());
        writeOdrlField(gen, provider, useNameSpacePrefix, "assigner", value.getAssigner());

        if (value.getProfile() != null) {
            gen.writeStringField("profile", value.getProfile());
        }

        gen.writeEndObject();
    }


    private void applyUseNamespaceFlagToPermissions(PolicyRequest value, boolean useNamespacePrefix) {
        if (value.getPermission() == null) return;
        for (PermissionRequest p : value.getPermission()) {
            if (p != null) p.setUseNameSpacePrefix(useNamespacePrefix);
        }
    }
}