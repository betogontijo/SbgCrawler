package br.com.betogontijo.sbgreader;

import java.io.IOException;

import br.com.betogontijo.sbgfetcher.SbgFetcher;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) throws IOException {
		// new SbgConsumer();
		SbgCrawler bfs = new SbgCrawler();
		for (int i = 0; i < args.length; i++) {
			bfs.crawl(args[i]);
		}
		bfs.close();
		SbgFetcher sbgFetcher = new SbgFetcher();
		sbgFetcher.fetch();
		sbgFetcher.close();
	}
}
