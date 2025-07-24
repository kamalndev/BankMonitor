package thekamaln.monitorproject.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.nusenda")
public class NusendaProperties {
    private String publicRoot;
    private String bankRoot;
    private String userId;
    private String password;
    private long mfaPollIntervalMs;
    private long mfaTimeoutMs;
}
