package thekamaln.monitorproject;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import thekamaln.monitorproject.scrapers.CapitalOneScraper;
import thekamaln.monitorproject.scrapers.DiscoverScraper;

@SpringBootApplication
public class MonitorprojectApplication implements CommandLineRunner {

	private final CapitalOneScraper capitalOneScraper;
	private final DiscoverScraper discoverScraper;

	public MonitorprojectApplication(CapitalOneScraper capitalOneScraper, DiscoverScraper discoverScraper) {
		this.capitalOneScraper = capitalOneScraper;
		this.discoverScraper = discoverScraper;
	}

	public static void main(String[] args) {
		SpringApplication.run(MonitorprojectApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
//		System.out.println("Fetching CapitalOne checking balance…");
//		System.out.println("Balance: $" + capitalOneScraper.getCheckingBalance());
		System.out.println("Fetching Discover checking balance");
		System.out.println("Balance: $" + discoverScraper.getCreditBalance());
		System.exit(0);
	}
}
