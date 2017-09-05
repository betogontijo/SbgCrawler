package br.com.betogontijo.sbgreader;

import java.io.Closeable;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class SbgVocabulary implements Closeable {
	SbgLetter root;
	MongoClient mongoClient;
	MongoCollection<SbgLetter> collection;

	SbgVocabulary(SbgLetter root) {
		this.root = root;
		init();
	}

	public void init() {
		mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase database = mongoClient.getDatabase("SbgDB");
		collection = database.getCollection("database", SbgLetter.class);
		// BasicDBObject data = new BasicDBObject();
		// data.put("letter", root.getLetter());
		// buildVocabulary(root, data);

		// collection.updateOne(collection.find(root).first(), root);
		System.out.println();
	}

	void buildVocabulary(SbgLetter sbgLetter, BasicDBObject previous) {
		List<String> pages = sbgLetter.getPages();
		if (pages != null) {
			for (int i = 0; i < pages.size(); i++) {
				SbgLetter nextLetter = sbgLetter.getNextLetters().get(i);
				BasicDBObject next = new BasicDBObject();
				next.put("letter", nextLetter.getLetter());
				next.put("pages", nextLetter.getPages());
				// List<BasicDBObject> object = (List<BasicDBObject>)
				// previous.get("nextLetters");
				previous.put("nextLetters", next);
				buildVocabulary(nextLetter, next);
			}
		}
	}

	void addPage(String word, String page) {
		// BasicDBObject next = new BasicDBObject();
		// next.put("letter", root.getLetter());
		// for (int i = 0; i < word.length(); i++) {
		// BasicDBObject tmp = new BasicDBObject();
		// tmp.put("letter", word.charAt(i));
		// next.put("nextLetters", value);
		// }
		// Document first = collection.find(query).first();
	}

	public void close() {
		mongoClient.close();
	}

}
