package org.eclipse.tractusx.sde.common.configuration.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "edc.version")
@Data
public class EDCVersionConfigurationProperties {

    @Min(0)
    @Max(100)
    private int major=0;

    @Min(0)
    @Max(100)
    private int minor=11;

    @Min(0)
    @Max(100)
    private int nano=2;
}
