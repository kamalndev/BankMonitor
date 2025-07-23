package thekamaln.monitorproject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import thekamaln.monitorproject.scrapers.NusendaScraper;

@SpringBootApplication
public class MonitorprojectApplication implements CommandLineRunner {

	@Autowired
	private NusendaScraper scraper;

	public static void main(String[] args) {
		SpringApplication.run(MonitorprojectApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Fetching Nusenda balance…");
		System.out.println("Balance: " + scraper.getBalance());
		// Exit now that we’re done
		System.exit(0);
	}
}
