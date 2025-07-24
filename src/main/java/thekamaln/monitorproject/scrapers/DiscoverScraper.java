package thekamaln.monitorproject.scrapers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v114.page.Page;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import thekamaln.monitorproject.properties.DiscoverProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
public class DiscoverScraper {
    private final DiscoverProperties props;

    public DiscoverScraper(DiscoverProperties props) {
        this.props = props;
    }

    /**
     * Logs in to Discover and returns your current credit balance.
     */
    public BigDecimal getCreditBalance() throws Exception {
        // 1) Boot ChromeDriver
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        // Optional: hide “Chrome is being controlled by automated test software”
        opts.addArguments("--disable-blink-features=AutomationControlled");
        ChromeDriver driver = new ChromeDriver(opts);

        // 2) Stealth: inject JS on every new page before any script runs
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        String stealthJs = """
            Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
            Object.defineProperty(navigator, 'plugins',   { get: () => [1,2,3,4,5] });
            Object.defineProperty(navigator, 'languages', { get: () => ['en-US','en'] });
            window.chrome = { runtime: {} };
            """;
        devTools.send(Page.addScriptToEvaluateOnNewDocument(
                stealthJs,
                Optional.empty(),
                Optional.empty()
        ));

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // 3) Navigate & log in
            driver.get(props.getPublicRoot());  // e.g. "https://www.discover.com/"
            wait.until(ExpectedConditions
                            .visibilityOfElementLocated(By.id("user-id-input")))
                    .sendKeys(props.getUserId());
            wait.until(ExpectedConditions
                            .visibilityOfElementLocated(By.id("password-input")))
                    .sendKeys(props.getPassword());
            wait.until(ExpectedConditions
                            .elementToBeClickable(By.cssSelector("button[data-testid='log-in']")))
                    .click();

            // 4) Wait for the post‑login page
            wait.until(ExpectedConditions.urlContains("/web/achome/homepage"));

            // 5) Scrape the balance
            String raw = wait.until(ExpectedConditions
                            .visibilityOfElementLocated(By.className("current-balance-text")))
                    .getText();  // e.g. "$1,234.56"

            // 6) Cleanup & parse
            String cleaned = raw.replaceAll("[$,]", "");  // "1234.56"
            return new BigDecimal(cleaned);
        } finally {
            driver.quit();
        }
    }
}
