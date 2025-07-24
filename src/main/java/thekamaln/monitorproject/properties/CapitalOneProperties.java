package thekamaln.monitorproject.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.capitalone")
public class CapitalOneProperties {
    private String publicRoot;
    private String userId;
    private String password;
}
