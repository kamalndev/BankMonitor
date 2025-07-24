package thekamaln.monitorproject.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix="app.discover")
public class DiscoverProperties {
    private String publicRoot;
    private String userId;
    private String password;
}
