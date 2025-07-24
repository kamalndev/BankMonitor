package thekamaln.monitorproject.scrapers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import thekamaln.monitorproject.properties.CapitalOneProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CapitalOneScraper {
    private final CapitalOneProperties props;

    public CapitalOneScraper(CapitalOneProperties props) {
        this.props = props;
    }

    /**
     * Logs in to CapitalOne and returns checking account's available balance.
     */
    public BigDecimal getCheckingBalance() throws Exception {
        // setup ChromeDriver
        WebDriverManager.chromedriver().setup();

        // launch Chrome
        ChromeOptions opts = new ChromeOptions();
        WebDriver driver = new ChromeDriver(opts);


        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            // go to homepage
            driver.get(props.getPublicRoot());

            // fill in username
            wait.until(ExpectedConditions
                            .visibilityOfElementLocated(By.id("ods-input-0")))
                    .sendKeys(props.getUserId());

            // fill in password
            wait.until(ExpectedConditions
                            .visibilityOfElementLocated(By.id("ods-input-1")))
                    .sendKeys(props.getPassword());

            // Click "Sign In"
            driver.findElement(By.id("noAcctSubmit")).click();

            // wait till on screen with balance
            wait.until(ExpectedConditions.urlContains("/accountSummary"));

            // locate balance string
            String srText = wait.until(ExpectedConditions
                            .visibilityOfElementLocated(By.xpath(
                                    "//span[contains(@class,'sr-only') and starts-with(normalize-space(.),'AVAILABLE BALANCE')]"
                            )))
                    .getText();


            // extract number
            Matcher m = Pattern.compile("(\\d+[\\d,]*\\.\\d{2})").matcher(srText);
            if (!m.find()) {
                throw new IllegalStateException("Could not parse balance from text: " + srText);
            }

            // remove any commas, convert to BigDecimal
            String numeric = m.group(1).replace(",", "");
            return new BigDecimal(numeric);


        } finally {

            driver.quit();
        }
    }
}
