package br.com.betogontijo.sbgreader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hello world!
 *
 */
public class SbgReaderMain {
	private static Thread monitor;

	public static void main(String[] args) throws IOException, InterruptedException {
		final SbgCrawler bfs = new SbgCrawler();
		performanceMonitor(bfs);

		ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(16);

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
				// FIXME as referencias tem q ficar em disco
				newFixedThreadPool.execute(bfs);
				Thread.sleep(200);
			} catch (Exception e) {
				//Should be ignored?
			}
		}
		bfs.close();
		monitor.interrupt();
	}

	private static void performanceMonitor(final SbgCrawler bfs) {
		//Create a thread for monitoring insertions/second
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
