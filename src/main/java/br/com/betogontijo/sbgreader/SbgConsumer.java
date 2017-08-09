package br.com.betogontijo.sbgreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SbgConsumer {

	private Map<String, List<String>> queryCache;
	private SbgResultSet resultSet;
	private boolean finished;
	File consumerCache = new File("consumer.dat");

	SbgConsumer() {
		loadCache();
		resultSet = new SbgResultSet();
		setFinished(false);
	}

	@SuppressWarnings("unchecked")
	private void loadCache() {
		try {
			FileInputStream fileInputStream = new FileInputStream(consumerCache);
			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

			queryCache = (HashMap<String, List<String>>) objectInputStream.readObject();
			objectInputStream.close();
		} catch (Exception e) {
			queryCache = new HashMap<String, List<String>>();
		}
	}

	public SbgResultSet consume(String query) {
		List<String> cache = queryCache.get(query);
		if (cache != null) {
			resultSet.setFetchResult(cache);
			setFinished(true);
		} else {
			SbgProducer producer = new SbgProducer();
			List<String> fetchResult = producer.produce(query);
			resultSet.setFetchResult(fetchResult);
			resultSet.setFinished(true);
			addCache(query, fetchResult);
		}
		return resultSet;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
		resultSet.setFinished(finished);
	}

	public void addCache(String query, List<String> fetchResult) {
		queryCache.put(query, fetchResult);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(consumerCache);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

			objectOutputStream.writeObject(queryCache);
			objectOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
