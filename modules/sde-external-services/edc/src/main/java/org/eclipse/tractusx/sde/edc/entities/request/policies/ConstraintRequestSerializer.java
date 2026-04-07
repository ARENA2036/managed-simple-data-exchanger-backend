package org.eclipse.tractusx.sde.edc.entities.request.policies;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import static org.eclipse.tractusx.sde.edc.entities.request.policies.OdrlFieldWriter.writeOdrlField;

public class ConstraintRequestSerializer extends StdSerializer<ConstraintRequest> {

    public ConstraintRequestSerializer() {
        super(ConstraintRequest.class);
    }

    @Override
    public void serialize(ConstraintRequest value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        boolean useNameSpacePrefix = value.isUseNameSpacePrefix();

        gen.writeStartObject();

        writeOdrlField(gen, provider, useNameSpacePrefix, "leftOperand", value.getLeftOperand());
        writeOdrlField(gen, provider, useNameSpacePrefix, "rightOperand", value.getRightOperand());
        writeOdrlField(gen, provider, useNameSpacePrefix, "operator", value.getOperator());

        gen.writeEndObject();
    }


}
