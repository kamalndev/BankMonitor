package thekamaln.monitorproject.scrapers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import thekamaln.monitorproject.apis.GmailAuthentication;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Service
public class NusendaScraper {

   private final NusendaProperties props;
   private final GmailAuthentication gmail;

   public NusendaScraper(NusendaProperties props, GmailAuthentication gmail) {
       this.props = props;
       this.gmail = gmail;
   }

   public BigDecimal getBalance() throws Exception {

       // chromedriver setup
       WebDriverManager.chromedriver().setup();
       ChromeOptions options = new ChromeOptions();

       WebDriver driver = new ChromeDriver(options);
       WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

       try {
           driver.get(props.getPublicRoot());
           WebElement iframe = wait.until(
                   ExpectedConditions.presenceOfElementLocated(
                           By.cssSelector("iframe.online-banking")
                   )
           );
           driver.switchTo().frame(iframe);

           wait.until(ExpectedConditions
                   .visibilityOfElementLocated(By.id("user_id")))
                   .sendKeys(props.getUserId());
           driver.findElement(By.id("password"))
                   .sendKeys(props.getPassword());
           driver.findElement(By.xpath("//input[@type='submit']"))
                   .click();

           // wait until we get to 2fa screen
           wait.until(ExpectedConditions.urlContains("/login/mfa/targets"));
           // clicks email code button
           List<WebElement> targets = wait.until(
                   ExpectedConditions.visibilityOfAllElementsLocatedBy(
                           By.cssSelector("button[test-id='btnTacTarget']")
                   )
           );

           for (WebElement t : targets) {
               String txt = t.getText().trim();
               if (txt.toLowerCase().contains("e-mail")) {
                   // scroll into view in case it's offscreen
                   ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", t);
                   t.click();
                   break;
               }
           }


           // pause for code to be sent
           Thread.sleep(3000);
           String code = null;
           long start = System.currentTimeMillis();
           while(System.currentTimeMillis() - start < props.getMfaTimeoutMs()) {
               code = gmail.fetchLatestCode();
               if(code != null) {break;}
               Thread.sleep(props.getMfaPollIntervalMs());
           }

           if (code == null) {
               throw new Exception("Code not found");
           }

           // now enter code and click submit
           wait.until(ExpectedConditions.visibilityOfElementLocated(
                   By.cssSelector("input#tacEntry")))
                   .sendKeys(code);
           wait.until(ExpectedConditions.elementToBeClickable(
                   By.cssSelector("button[test-id='btnSubmit']")
           ))
                   .click();
           // click "Do Not Register"

           wait.until(ExpectedConditions.urlContains("/login/mfa/register"));
           wait.until(ExpectedConditions.elementToBeClickable(
                   By.cssSelector("button[test-id='btnDoNotRegister']")
           ))
                   .click();

           // now we are at landing page, so scrape balance and return
           wait.until(ExpectedConditions.urlContains("/landingPage"));
           List<WebElement> balances = wait.until(
                   ExpectedConditions.visibilityOfAllElementsLocatedBy(
                           By.cssSelector("span.numAmount")
                   )
           );
           String raw = balances.get(0).getText();
           // clean string, remove $ + commas and parse to bigdecimal

           String cleaned = raw.replaceAll("[^0-9.\\-]", "");
           return new BigDecimal(cleaned);


       } finally {
           driver.quit();
       }
   }
}
