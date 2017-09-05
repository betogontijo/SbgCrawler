package br.com.betogontijo.sbgfetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.ws.WebServiceException;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.sun.xml.internal.ws.Closeable;

import br.com.betogontijo.sbgreader.SbgMap;
import br.com.betogontijo.sbgreader.SbgPage;

public class SbgFetcher implements Closeable {

	MongoClient mongoClient;
	@SuppressWarnings("rawtypes")
	MongoCollection<Map> pageDB;

	public SbgFetcher() {
		loadCache();
	}

	private void loadCache() {
		mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase database = mongoClient.getDatabase("SbgDB");
		pageDB = database.getCollection("page", Map.class);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void fetch() {
		Scanner in = new Scanner(System.in);
		String query = in.nextLine();
		List<String> words = new ArrayList<String>();
		Scanner stream = new Scanner(query);
		while (stream.hasNext()) {
			String word = stream.next();
			if (word.startsWith("\"") && query.replace("\"", "").length() >= 2) {
				while (!word.endsWith("\"")) {
					word += " " + stream.next();
				}
				word = word.substring(1, word.length() - 1);
				query = query.replaceFirst("\"", "");
				query = query.replaceFirst("\"", "");
			}
			words.add(word);
		}

		List<Integer> matches = new ArrayList<Integer>();
		MongoCursor<SbgMap> iterator = pageDB.find(SbgMap.class).iterator();
		int i = 0;
		while (iterator.hasNext()) {
			SbgPage page = new SbgPage(iterator.next());
			matches.add(0);
			for (int j = 0; j < words.size(); j++) {
				if (page.containsWord(words.get(j)) > 0) {
					matches.set(i, matches.get(i) + 1);
				}
			}
			System.out.println(page.getPath() + " -> " + matches.get(i++));
		}
		in.close();
		stream.close();
	}

	public void close() throws WebServiceException {
		mongoClient.close();
	}
}