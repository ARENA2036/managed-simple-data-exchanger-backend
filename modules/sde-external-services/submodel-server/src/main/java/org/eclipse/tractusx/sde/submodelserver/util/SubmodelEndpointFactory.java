package org.eclipse.tractusx.sde.submodelserver.util;

import java.net.URI;

public interface SubmodelEndpointFactory {
    String createAsString();
    URI createURI();
}
