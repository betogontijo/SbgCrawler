package br.com.betogontijo.sbgreader;

import java.io.IOException;
import java.net.URISyntaxException;

import br.com.betogontijo.sbgfetcher.SbgFetcher;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) throws IOException {
		// new SbgConsumer();
		final SbgCrawler bfs = new SbgCrawler();
		performanceMonitor(bfs);
		
		//Loop through arguments used as seeds
		for (int i = 0; i < args.length; i++) {
			try {
				String uri = UriUtils.pathToUri(args[i]).toString();
				bfs.crawl(uri, null);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		bfs.close();
		
		
		SbgFetcher sbgFetcher = new SbgFetcher();
		long fetchStart = System.currentTimeMillis();
		sbgFetcher.fetch();
		long fetchEnd = System.currentTimeMillis();
		sbgFetcher.close();
		System.out.println("Fetching time: " + (fetchEnd - fetchStart) + "ms.");
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
