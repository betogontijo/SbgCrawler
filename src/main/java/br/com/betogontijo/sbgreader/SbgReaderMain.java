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
	public static void main(String[] args) throws IOException, InterruptedException {
		final SbgCrawler bfs = new SbgCrawler();
		performanceMonitor(bfs);

		ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(1);
//		 newFixedThreadPool.awaitTermination(2, TimeUnit.SECONDS);

		// Loop through arguments used as seeds
		for (int i = 0; i < args.length; i++) {
			try {
				String uri = UriUtils.pathToUri(args[i]).toString();
				bfs.crawl(uri);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		while (!bfs.getReferences().isEmpty()) {
			try {
				//FIXME as referencias tem q ficar em disco
				newFixedThreadPool.execute(bfs);
			} catch (Exception e) {
				// Holds the exception, so the entire machine doesnt
				// stop.
			}
		}
		bfs.close();
	}

	private static void performanceMonitor(final SbgCrawler bfs) {
		new Thread() {
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
		}.start();
	}
}
