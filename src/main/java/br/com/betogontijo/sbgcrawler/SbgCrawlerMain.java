package br.com.betogontijo.sbgcrawler;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SbgCrawlerMain {

	@Autowired
	SbgDataSource dataSource;

	@Autowired
	PerformanceMonitor monitor;

	@Autowired
	SbgThreadPoolExecutor threadPoolExecutor;

	@Autowired
	SbgCrawler crawler;

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		SpringApplication.run(SbgCrawlerMain.class, args);
	}

	@Bean
	CommandLineRunner init(ConfigurableApplicationContext applitcationContext) {
		return args -> {
			consume(args);
		};

	}

	/**
	 * @param seeds
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("deprecation")
	void consume(String[] seeds) throws IOException, InterruptedException {
		monitor.start();

		Properties properties = new Properties();
		properties.load(ClassLoader.getSystemResourceAsStream("sbgcrawler.properties"));

		int threadNumber = Integer.parseInt(properties.getProperty("environment.threads"));

		// Loop through arguments used as seeds
		for (int i = 0; i < seeds.length; i++) {
			try {
				String uri = UriUtils.pathToUri(seeds[i]).toString();
				crawler.crawl(uri);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		while (dataSource.hasReferences()) {
			if (threadPoolExecutor.getActiveCount() < threadNumber) {
				threadPoolExecutor.execute(crawler);
			}
		}
		threadPoolExecutor.shutdown();
		monitor.stop();
	}
}
