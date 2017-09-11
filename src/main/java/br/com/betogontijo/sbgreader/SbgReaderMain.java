package br.com.betogontijo.sbgreader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Hello world!
 *
 */
public class SbgReaderMain {
	private static Thread monitor;

	public static void main(String[] args) throws IOException, InterruptedException {
		final SbgCrawler bfs = new SbgCrawler();
		performanceMonitor(bfs);

		int threadNumber = 16;

		ThreadPoolExecutor newFixedThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadNumber);

		// Loop through arguments used as seeds
		for (int i = 0; i < args.length; i++) {
			try {
				String uri = UriUtils.pathToUri(args[i]).toString();
				bfs.crawl(uri);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		while (bfs.hasReferences()) {
			try {
				if (newFixedThreadPool.getActiveCount() < threadNumber) {
					newFixedThreadPool.execute(bfs);
				}
			} catch (Exception e) {
				// Should be ignored?
			}
		}
		bfs.close();
		monitor.interrupt();
	}

	private static void performanceMonitor(final SbgCrawler bfs) {
		// Create a thread for monitoring insertions/second
		monitor = new Thread() {
			@Override
			public void run() {
				Integer atual = bfs.getDocIdCounter();
				double speed = 0;
				while (true) {
					try {
						Thread.sleep(1000);
						Integer next = bfs.getDocIdCounter();
						speed = ((next - atual) + speed) / 2;
						System.out.printf("\r%.2f", speed);
						atual = next;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		monitor.start();
	}
}
