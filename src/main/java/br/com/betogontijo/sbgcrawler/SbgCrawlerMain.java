package br.com.betogontijo.sbgcrawler;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
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

		CountDownLatch latch = new CountDownLatch(threadNumber);

		crawler = new SbgCrawler(dataSource, latch);
		
		// Loop through arguments used as seeds
		for (int i = 0; i < seeds.length; i++) {
			try {
				crawler.crawl(UriUtils.pathToUri(seeds[i]).toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// String root = "D:/WT10G/";
		// for (String folder : new File(root).list()) {
		// for (String subFolder : new File(root + folder).list()) {
		// String subFolderPath = root + folder + "/" + subFolder;
		// List<String> references = new ArrayList<String>();
		// for (String file : new File(subFolderPath).list()) {
		// references.add(subFolderPath + "/" + file);
		// }
		// dataSource.insertReference(references);
		// }
		// }
		threadPoolExecutor = new SbgThreadPoolExecutor(threadNumber);
		
		while (threadPoolExecutor.getActiveCount() < threadNumber) {
			threadPoolExecutor.execute(crawler);
		}
		latch.await();
		crawler.setCanceled(true);
	}

	@PreDestroy
	public void onDestroy() {
		System.out.println("Waiting all collectors to end...");
		crawler.setCanceled(true);
		monitor.cancel();
		try {
			boolean awaitTermination = threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);
			if (awaitTermination) {
				System.out.println("All collectors have finished.");
			} else {
				System.out.println("Collectors was forced finishing, timeout reached.");
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Shutting down...");
		System.out.println(
				"Writing " + dataSource.getReferencesBufferQueue().size() + " references from buffer on disk...");
		dataSource.saveRefsOnDisk(dataSource.getReferencesBufferQueue().size());
		threadPoolExecutor.shutdownNow();
		System.out.println("Shutdown, buffer is down to " + dataSource.getReferencesBufferQueue().size() + ".");
	}
}
