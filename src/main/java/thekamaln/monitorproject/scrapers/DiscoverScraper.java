package thekamaln.monitorproject.scrapers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import thekamaln.monitorproject.properties.DiscoverProperties;
import thekamaln.monitorproject.util.CookieStore;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DiscoverScraper {

    private final DiscoverProperties props;

    public DiscoverScraper(DiscoverProperties props) {
        this.props = props;
    }

    /**
     * Returns your Discover current credit balance.
     * - First run: mode=ATTACH -> you're already logged in in a human Chrome, we save cookies.
     * - Later runs: mode=NORMAL -> we load cookies and skip login.
     */
    public BigDecimal getCreditBalance() {
        WebDriver driver = null;
        try {
            driver = buildDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            Path cookiePath = Path.of(props.getCookieFile());

            // Try to restore cookies first (so we skip login in NORMAL mode)
            try {
                CookieStore.load(driver, cookiePath, props.getPublicRoot());
            } catch (Exception ignored) {}

            boolean didLoginThroughForm = false;

            if (!isLoggedIn(driver)) {
                loginThroughForm(driver, wait);
                didLoginThroughForm = true;
            }

            // If we either just logged in OR we're in ATTACH mode (already logged in manually),
            // save cookies so we can reuse them in NORMAL mode later.
            if (didLoginThroughForm || props.getMode() == DiscoverProperties.Mode.ATTACH) {
                try {
                    CookieStore.save(driver, cookiePath);
                } catch (Exception ignored) {}
            }

            wait.until(ExpectedConditions.urlContains("/web/achome"));

            String raw = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.className("current-balance-text"))
            ).getText();

            String cleaned = raw.replaceAll("[$,]", "");
            return new BigDecimal(cleaned);

        } finally {
            // In ATTACH mode, don't kill the human-opened browser.
            if (driver != null && props.getMode() == DiscoverProperties.Mode.NORMAL) {
                driver.quit();
            }
        }
    }

    // ------------------------------------------------------------------------

    private WebDriver buildDriver() {
        ChromeOptions options = new ChromeOptions();

        if (props.isHeadless()) {
            options.addArguments("--headless=new");
        }

        if (props.getMode() == DiscoverProperties.Mode.ATTACH) {
            // Attach to the Chrome you started with --remote-debugging-port
            options.setExperimentalOption("debuggerAddress", props.getDebuggerAddress());
            return new ChromeDriver(options);
        }

        // NORMAL mode: launch our own Chrome
        WebDriverManager.chromedriver().setup();

        // Reduce obvious automation fingerprints
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--lang=en-US,en");
        options.addArguments("--start-maximized");

        // Persistent profile to look consistent (optional but recommended)
        options.addArguments("--user-data-dir=" + props.getUserDataDir());
        options.addArguments("--profile-directory=" + props.getProfileDirectory());

        return new ChromeDriver(options);
    }

    private boolean isLoggedIn(WebDriver driver) {
        try {
            if (driver.getCurrentUrl() != null && driver.getCurrentUrl().contains("/web/achome")) {
                return true;
            }
            return !driver.findElements(By.className("current-balance-text")).isEmpty();
        } catch (Exception ex) {
            return false;
        }
    }

    private void loginThroughForm(WebDriver driver, WebDriverWait wait) {
        driver.get(props.getPublicRoot());

        // Late webdriver shim (not perfect, but helps a tad without DevTools)
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', { get: () => undefined })"
            );
        } catch (JavascriptException ignored) {}

        WebElement userIdInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("user-id-input")));
        slowType(userIdInput, props.getUserId());
        sleepBetween(150, 350);

        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password-input")));
        slowType(passwordInput, props.getPassword());
        sleepBetween(150, 350);

        WebElement loginButton =
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-testid='log-in']")));
        loginButton.click();

        if (isBlocked(driver)) {
            throw new RuntimeException("Discover blocked login, run in ATTACH mode + save cookies");
        }

        wait.until(ExpectedConditions.urlContains("/web/achome"));
    }

    private boolean isBlocked(WebDriver driver) {
        return !driver.findElements(
                By.xpath("//*[contains(text(),\"We're sorry. Your request cannot be completed\")]")
        ).isEmpty();
    }

    private void slowType(WebElement element, String text) {
        for (char c : text.toCharArray()) {
            element.sendKeys(String.valueOf(c));
            sleepBetween(50, 150);
        }
    }

    private void sleepBetween(int min, int max) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(min, max + 1));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
