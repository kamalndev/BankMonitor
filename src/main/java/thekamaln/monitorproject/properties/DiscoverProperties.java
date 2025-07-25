package thekamaln.monitorproject.properties;

import lombok.Data;
import org.springframework.boot.Banner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix="app.discover")

public class DiscoverProperties {
    private String publicRoot;
    private String userId;
    private String password;

    public enum Mode {
        NORMAL,
        ATTACH
    }
    private Mode mode = Mode.NORMAL;

    private String debuggerAddress = "127.0.0.1:9222";

    private String cookieFile = System.getProperty("user.home") + "/.discover.cookies.json";
    private String userDataDir = System.getProperty("user.home") + "/.discover-chrome-profile";
    private String profileDirectory = "Default";
    private boolean headless = false;



}
