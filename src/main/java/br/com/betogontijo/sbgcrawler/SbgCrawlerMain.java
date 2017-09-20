package br.com.betogontijo.sbgcrawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class SbgCrawlerMain {

	SbgDataSource dataSource = SbgDataSource.getInstance();

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		SbgCrawlerMain crawler = new SbgCrawlerMain();
		crawler.consume(args);
	}

	/**
	 * @param seeds
	 * @throws IOException
	 * @throws InterruptedException
	 */
	void consume(String[] seeds) throws IOException, InterruptedException {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.start();

		Properties properties = new Properties();
		properties.load(ClassLoader.getSystemResourceAsStream("sbgcrawler.properties"));

		int threadNumber = Integer.parseInt(properties.getProperty("environment.threads"));

		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadNumber);

		final SbgCrawler crawler = new SbgCrawler();

		List<String> seedsReferences = new ArrayList<String>();
		// Loop through arguments used as seeds
		for (int i = 0; i < seeds.length; i++) {
			try {
				String uri = UriUtils.pathToUri(seeds[i]).toString();
				seedsReferences.add(uri);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		dataSource.insertReference(seedsReferences);

		while (dataSource.hasReferences()) {
			if (threadPoolExecutor.getActiveCount() < threadNumber) {
				threadPoolExecutor.execute(crawler);
			}
		}
		while (!threadPoolExecutor.isShutdown()) {
			monitor.cancel();
			Thread.sleep(10000);
		}
	}
}
