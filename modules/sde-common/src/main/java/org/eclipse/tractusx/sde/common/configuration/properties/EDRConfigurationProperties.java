package org.eclipse.tractusx.sde.common.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "edr")
@Data
public class EDRConfigurationProperties {

    private Retry retry = new Retry();
    private Refresh refresh = new Refresh();

    @Data
    public static class Retry {
        // amount of max retries
        private Integer count = 5;
        //Time in ms
        private Integer time = 250;
    }

    @Data
    public static class Refresh {
        //enable/disable auto refresh of edr token
        private Boolean auto = false;
    }
}
