package br.com.betogontijo.sbgcrawler;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

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

	static ConfigurableApplicationContext run;

	@Autowired
	SbgDocumentRepository documentRepository;

	SbgThreadPoolExecutor threadPoolExecutor;

	SbgCrawlerDao dataSource;

	SbgCrawlerPerformanceMonitor monitor;

	SbgCrawler crawler;

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		run = SpringApplication.run(SbgCrawlerMain.class, args);
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
	void consume(String[] seeds) throws Exception {
		Properties properties = new Properties();
		properties.load(ClassLoader.getSystemResourceAsStream("sbgcrawler.properties"));
		int threadNumber = Integer.parseInt(properties.getProperty("environment.threads"));
		int bufferSize = Integer.parseInt(properties.getProperty("environment.buffer.size"));
		dataSource = new SbgCrawlerDao(threadNumber, bufferSize, documentRepository);

		monitor = new SbgCrawlerPerformanceMonitor(dataSource);
		monitor.start();

		crawler = new SbgCrawler(dataSource);

		// Loop through arguments used as seeds
		for (int i = 0; i < seeds.length; i++) {
			try {
				crawler.crawl(UriUtils.pathToUri(seeds[i]).toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		threadPoolExecutor = new SbgThreadPoolExecutor(threadNumber);
		while (!crawler.isCanceled()) {
			if (threadPoolExecutor.getActiveCount() < threadNumber) {
				threadPoolExecutor.execute(crawler);
			}
		}
		threadPoolExecutor.shutdown();
		monitor.cancel();
	}

	@PreDestroy
	public void onDestroy() {
		System.out.println("Shutting down...");
		crawler.setCanceled(true);
		try {
			threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Writing "+dataSource.getReferencesBufferQueue().size()+" references from buffer on disk...");
		dataSource.saveRefsOnDisk(dataSource.getReferencesBufferQueue().size());
		monitor.cancel();
		System.out.println("Shutdown, buffer is down to " + dataSource.getReferencesBufferQueue().size() + ".");
	}
}
