package br.com.betogontijo.sbgreader;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;

public class SbgDataSource {

	static final String INSERT_REFERENCE_QUERY = "INSERT INTO refs (uri) VALUES ";
	static final String SELECT_AND_REMOVE_REFERENCE_QUERY = "DELETE FROM refs LIMIT ? RETURNING uri";

	private AtomicInteger docIdCounter = new AtomicInteger(1);

	private ConnectionManager connectionFactory;

	@SuppressWarnings("rawtypes")
	MongoCollection<Map> domainDB;

	@SuppressWarnings("rawtypes")
	MongoCollection<Map> documentDB;

	private Connection mariaDbConnection;

	private Queue<String> referencesBufferQueue;

	private static int bufferSize;

	private static int threads;

	{
		// Starts connectionFactory
		connectionFactory = new ConnectionManager();

		// Get the connection for both document and domain collections
		domainDB = connectionFactory.getDomainsConnection();
		documentDB = connectionFactory.getDocumentsConnection();

		// Get the connection for references database
		setMariaDbConnection(connectionFactory.getReferencesConnection());

		// Query to search for the last documentID
		BasicDBObject id = new BasicDBObject();
		id.put("_id", -1);
		@SuppressWarnings("unchecked")
		Map<String, Object> maxId = documentDB.find().sort(id).first();
		if (maxId != null) {
			setDocIdCounter((Integer) maxId.get("_id"));
		}

		setReferencesBufferQueue(new ConcurrentLinkedQueue<String>());
		Properties properties = new Properties();
		try {
			properties.load(ClassLoader.getSystemResourceAsStream("sbgreader.properties"));
			setBufferSize(Integer.parseInt(properties.getProperty("environment.buffer.size")));
			setThreads(Integer.parseInt(properties.getProperty("environment.threads")));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static SbgDataSource dataSource;

	public static SbgDataSource getInstance() {
		if (dataSource == null) {
			dataSource = new SbgDataSource();
		}
		return dataSource;
	}

	private Connection getMariaDbConnection() {
		return mariaDbConnection;
	}

	private void setMariaDbConnection(Connection mariaDbConnection) {
		this.mariaDbConnection = mariaDbConnection;
	}

	public Queue<String> getReferencesBufferQueue() {
		return referencesBufferQueue;
	}

	private void setReferencesBufferQueue(Queue<String> referencesBufferQueue) {
		this.referencesBufferQueue = referencesBufferQueue;
	}

	private static int getBufferSize() {
		return bufferSize;
	}

	private static void setBufferSize(int bufferSize) {
		SbgDataSource.bufferSize = bufferSize;
	}

	private static int getThreads() {
		return threads;
	}

	private static void setThreads(int threads) {
		SbgDataSource.threads = threads;
	}

	public int getDocIdCounter() {
		return docIdCounter.get();
	}

	private void setDocIdCounter(int docIdCounter) {
		this.docIdCounter.set(docIdCounter);
	}

	public void updateDomainsDb(SbgMap<String, Object> document, SbgMap<String, Object> nextDocument) {
		if (nextDocument.get("_id") == null) {
			nextDocument.put("_id", docIdCounter.incrementAndGet());
			domainDB.insertOne(nextDocument);
		} else {
			domainDB.replaceOne(document, nextDocument);
		}
	}

	public void updateDocumentsDb(SbgMap<String, Object> document, SbgMap<String, Object> nextDocument) {
		if (nextDocument.get("_id") == null) {
			nextDocument.put("_id", docIdCounter.incrementAndGet());
			documentDB.insertOne(nextDocument);
		} else {
			documentDB.replaceOne(document, nextDocument);
		}
	}

	@SuppressWarnings("unchecked")
	public Domain findDomain(Domain domain) {
		Map<String, Object> domainMap = domainDB.find(domain, SbgMap.class).first();
		Domain nextDomain = new Domain(domainMap, domain.getUri());
		return nextDomain;
	}

	@SuppressWarnings("unchecked")
	public SbgDocument findDocument(SbgDocument sbgPage, boolean search) {
		SbgDocument nextSbgPage = null;
		if (search) {
			Map<String, Object> sbgPageMap = documentDB.find(sbgPage, SbgMap.class).first();
			nextSbgPage = new SbgDocument(sbgPageMap, sbgPage.getPath());
		} else {
			nextSbgPage = new SbgDocument(sbgPage.getPath());
		}
		return nextSbgPage;
	}

	public int increaseAndGetDocumentId() {
		return docIdCounter.incrementAndGet();
	}

	private int getBufferPerThread() {
		return getBufferSize() / getThreads() * 2;
	}

	public void insertReference(String reference) {
		getReferencesBufferQueue().add(reference);
		if (getReferencesBufferQueue().size() > getBufferSize()) {
			int n = getBufferPerThread();
			if (n > 0) {
				try {
					Statement createStatement = mariaDbConnection.createStatement();
					while (n > 0) {
						StringBuilder builder = new StringBuilder(INSERT_REFERENCE_QUERY);
						for (int i = 1024000; builder.length() < i && n > 0; n--) {
							builder.append("('");
							builder.append(getReferencesBufferQueue().remove());
							builder.append("'),");
						}
						builder.deleteCharAt(builder.length() - 1);
						createStatement.addBatch(builder.toString());
					}
					createStatement.executeBatch();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchElementException e1) {
				}
			}
		}
	}

	public String getReference() {
		// When reaches size of the buffer is time to fill it again
		if (getReferencesBufferQueue().size() < getBufferPerThread()) {
			try {
				PreparedStatement prepareStatement = getMariaDbConnection()
						.prepareStatement(SELECT_AND_REMOVE_REFERENCE_QUERY);
				prepareStatement.setInt(1, getBufferPerThread());
				ResultSet executeQuery = prepareStatement.executeQuery();
				while (executeQuery.next()) {
					getReferencesBufferQueue().add(executeQuery.getString("uri"));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (getReferencesBufferQueue().size() > 0) {
			return getReferencesBufferQueue().remove();
		} else {
			return null;
		}
	}

	public boolean hasReferences() {
		if (getReferencesBufferQueue().size() == 0) {
			String ref = getReference();
			if (ref == null) {
				return false;
			} else {
				getReferencesBufferQueue().add(ref);
				return true;
			}
		} else {
			return true;
		}
	}

}
