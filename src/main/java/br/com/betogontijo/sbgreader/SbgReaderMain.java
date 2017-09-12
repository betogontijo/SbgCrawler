package br.com.betogontijo.sbgreader;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class SbgReaderMain {

	SbgDataSource dataSource = SbgDataSource.getInstance();

	public static void main(String[] args) throws IOException, InterruptedException {
		SbgReaderMain reader = new SbgReaderMain();
		reader.consume(args);
	}

	void consume(String[] seeds) throws IOException, InterruptedException {
		PerformanceMonitor monitor = new PerformanceMonitor();
		monitor.start();

		final SbgCrawler crawler = new SbgCrawler();

		Properties properties = new Properties();
		properties.load(ClassLoader.getSystemResourceAsStream("sbgreader.properties"));

		int threadNumber = Integer.parseInt(properties.getProperty("environment.threads"));

		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadNumber);

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
		while (!threadPoolExecutor.isShutdown()) {
			monitor.cancel();
			Thread.sleep(10000);
		}
	}
}
