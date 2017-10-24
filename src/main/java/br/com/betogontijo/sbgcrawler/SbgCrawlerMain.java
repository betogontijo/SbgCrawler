package br.com.betogontijo.sbgcrawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import br.com.betogontijo.sbgbeans.crawler.repositories.SbgDocumentRepository;

@SpringBootApplication
@EnableMongoRepositories("br.com.betogontijo.sbgbeans.crawler.repositories")
public class SbgCrawlerMain {

	@Autowired
	SbgDocumentRepository documentRepository;

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
	void consume(String[] seeds) throws Exception {
		Properties properties = new Properties();
		properties.load(ClassLoader.getSystemResourceAsStream("sbgcrawler.properties"));
		int threadNumber = Integer.parseInt(properties.getProperty("environment.threads"));
		int bufferSize = Integer.parseInt(properties.getProperty("environment.buffer.size"));
		SbgCrawlerDao dataSource = new SbgCrawlerDao(threadNumber, bufferSize, documentRepository);

		SbgCrawlerPerformanceMonitor monitor = new SbgCrawlerPerformanceMonitor(dataSource);
		monitor.start();

		SbgCrawler crawler = new SbgCrawler(dataSource);

		List<String> references = new ArrayList<String>();
		// Loop through arguments used as seeds
		for (int i = 0; i < seeds.length; i++) {
			try {
				references.add(UriUtils.pathToUri(seeds[i]).toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		dataSource.insertReference(references);
		SbgThreadPoolExecutor threadPoolExecutor = new SbgThreadPoolExecutor(threadNumber);
		while (dataSource.hasReferences() || threadPoolExecutor.getActiveCount() > 0) {
			if (threadPoolExecutor.getActiveCount() < threadNumber) {
				threadPoolExecutor.execute(crawler);
			}
		}
		threadPoolExecutor.shutdown();
		monitor.stop();
	}
}
