package org.eclipse.tractusx.sde.edc.entities.request.policies;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import static org.eclipse.tractusx.sde.edc.entities.request.policies.OdrlFieldWriter.writeOdrlField;

public class PermissionRequestSerializer extends StdSerializer<PermissionRequest> {

    public PermissionRequestSerializer() {
        super(PermissionRequest.class);
    }

    @Override
    public void serialize(PermissionRequest value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        boolean useNameSpacePrefix = value.isUseNameSpacePrefix();

        gen.writeStartObject();

        writeOdrlField(gen, provider, useNameSpacePrefix, "action", value.getAction());
        writeOdrlField(gen, provider, useNameSpacePrefix, "constraint", value.getConstraint());

        gen.writeEndObject();
    }

}
