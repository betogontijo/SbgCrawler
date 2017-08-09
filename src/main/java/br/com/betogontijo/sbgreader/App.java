package br.com.betogontijo.sbgreader;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		new SbgConsumer();
		WebDigger bfs = new WebDigger();
		bfs.breadthFirstSearch(args[0]);
		System.out.println();
		System.out.println(bfs.getRanking().subList(0, 10));
		bfs.saveCache();
	}
}
