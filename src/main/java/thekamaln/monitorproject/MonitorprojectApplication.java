package thekamaln.monitorproject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import thekamaln.monitorproject.properties.DiscoverProperties;
import thekamaln.monitorproject.scrapers.DiscoverScraper;

import java.math.BigDecimal;

@SpringBootApplication
public class MonitorprojectApplication implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(MonitorprojectApplication.class);

	private final DiscoverScraper discoverScraper;
	private final DiscoverProperties discoverProps;

	public MonitorprojectApplication(DiscoverScraper discoverScraper,
									 DiscoverProperties discoverProps) {
		this.discoverScraper = discoverScraper;
		this.discoverProps = discoverProps;
	}

	public static void main(String[] args) {
		// Exit the JVM with the CommandLineRunner's exit code
		int code = SpringApplication.exit(SpringApplication.run(MonitorprojectApplication.class, args));
		System.exit(code);
	}

	@Override
	public void run(String... args) {
		log.info("==== Monitorproject starting ====");
		log.info("Discover mode: {}", discoverProps.getMode());
		log.info("Headless: {}", discoverProps.isHeadless());
		log.info("Cookie file: {}", discoverProps.getCookieFile());

		try {
			log.info("Fetching Discover credit balance…");
			BigDecimal balance = discoverScraper.getCreditBalance();
			log.info("Discover balance = ${}", balance);
		} catch (Exception e) {
			log.error("Discover scrape failed: {}", e.getMessage(), e);
			// non-zero means “failed” if you’re running this from a scheduler/CI
			System.exit(1);
		}

		log.info("Done.");
		System.exit(0);
	}
}
