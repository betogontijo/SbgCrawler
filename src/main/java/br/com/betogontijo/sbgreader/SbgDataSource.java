package br.com.betogontijo.sbgreader;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;

public class SbgDataSource {

	static final String INSERT_REFERENCE_QUERY = "INSERT INTO refs (uri) VALUES ";
	static final String SELECT_AND_REMOVE_REFERENCE_QUERY = "DELETE FROM refs LIMIT ? RETURNING uri";

	private AtomicInteger docIdCounter = new AtomicInteger(1);

	private ConnectionManager connectionFactory;

	@SuppressWarnings("rawtypes")
	MongoCollection<Map> domainsDb;

	@SuppressWarnings("rawtypes")
	MongoCollection<Map> documentsDb;

	private Connection mariaDbConnection;

	private Queue<String> referencesBufferQueue;

	private static int bufferSize;

	private static int threads;

	{
		try {
			// Starts connectionFactory
			connectionFactory = new ConnectionManager();

			// Get the connection for both document and domain collections
			domainsDb = connectionFactory.getDomainsConnection();
			documentsDb = connectionFactory.getDocumentsConnection();

			// Get the connection for references database
			setMariaDbConnection(connectionFactory.getReferencesConnection());

			// Query to search for the last documentID
			BasicDBObject id = new BasicDBObject();
			id.put("_id", -1);
			@SuppressWarnings("unchecked")
			Map<String, Object> maxId = documentsDb.find().sort(id).first();
			if (maxId != null) {
				setDocIdCounter((Integer) maxId.get("_id"));
			}

			setReferencesBufferQueue(new ConcurrentSetQueue<String>());
			Properties properties = new Properties();
			properties.load(ClassLoader.getSystemResourceAsStream("sbgreader.properties"));
			setBufferSize(Integer.parseInt(properties.getProperty("environment.buffer.size")));
			setThreads(Integer.parseInt(properties.getProperty("environment.threads")));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static SbgDataSource dataSource;

	/**
	 * @return
	 */
	public static SbgDataSource getInstance() {
		if (dataSource == null) {
			dataSource = new SbgDataSource();
		}
		return dataSource;
	}

	/**
	 * @return
	 */
	private Connection getMariaDbConnection() {
		return mariaDbConnection;
	}

	/**
	 * @param mariaDbConnection
	 */
	private void setMariaDbConnection(Connection mariaDbConnection) {
		this.mariaDbConnection = mariaDbConnection;
	}

	/**
	 * @return
	 */
	public Queue<String> getReferencesBufferQueue() {
		return referencesBufferQueue;
	}

	/**
	 * @param referencesBufferQueue
	 */
	private void setReferencesBufferQueue(Queue<String> referencesBufferQueue) {
		this.referencesBufferQueue = referencesBufferQueue;
	}

	/**
	 * @return
	 */
	private static int getBufferSize() {
		return bufferSize;
	}

	/**
	 * @param bufferSize
	 */
	private static void setBufferSize(int bufferSize) {
		SbgDataSource.bufferSize = bufferSize;
	}

	/**
	 * @return
	 */
	private static int getThreads() {
		return threads;
	}

	/**
	 * @param threads
	 */
	private static void setThreads(int threads) {
		SbgDataSource.threads = threads;
	}

	/**
	 * @return
	 */
	public int getDocIdCounter() {
		return docIdCounter.get();
	}

	/**
	 * @param docIdCounter
	 */
	private void setDocIdCounter(int docIdCounter) {
		this.docIdCounter.set(docIdCounter);
	}

	/**
	 * @param document
	 * @param nextDocument
	 */
	public void updateDomainsDb(SbgMap<String, Object> document, SbgMap<String, Object> nextDocument) {
		if (nextDocument.get("_id") == null) {
			nextDocument.put("_id", docIdCounter.incrementAndGet());
			domainsDb.insertOne(nextDocument);
		} else {
			domainsDb.replaceOne(document, nextDocument);
		}
	}

	/**
	 * @param document
	 * @param nextDocument
	 */
	public void updateDocumentsDb(SbgMap<String, Object> document, SbgMap<String, Object> nextDocument) {
		if (nextDocument.get("_id") == null) {
			nextDocument.put("_id", docIdCounter.incrementAndGet());
			documentsDb.insertOne(nextDocument);
		} else {
			documentsDb.replaceOne(document, nextDocument);
		}
	}

	/**
	 * @param domain
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Domain findDomain(Domain domain) {
		Map<String, Object> domainMap = domainsDb.find(domain, SbgMap.class).first();
		Domain nextDomain = new Domain(domainMap, domain.getUri());
		return nextDomain;
	}

	/**
	 * @param sbgPage
	 * @param search
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public SbgDocument findDocument(SbgDocument sbgPage, boolean search) {
		SbgDocument nextSbgPage = null;
		if (search) {
			Map<String, Object> sbgPageMap = documentsDb.find(sbgPage, SbgMap.class).first();
			nextSbgPage = new SbgDocument(sbgPageMap, sbgPage.getPath());
		} else {
			nextSbgPage = new SbgDocument(sbgPage.getPath());
		}
		return nextSbgPage;
	}

	/**
	 * @return
	 */
	public int increaseAndGetDocumentId() {
		return docIdCounter.incrementAndGet();
	}

	/**
	 * @return
	 */
	private int getBufferPerThread() {
		return getBufferSize() / getThreads() * 2;
	}

	/**
	 * @param reference
	 */
	public void insertReference(List<String> reference) {
		getReferencesBufferQueue().addAll((reference));
		if (getReferencesBufferQueue().size() > getBufferSize()) {
			int n = getBufferPerThread();
			if (n > 0) {
				try {
					Statement createStatement = getMariaDbConnection().createStatement();
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

	/**
	 * @return
	 */
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

	/**
	 * @return
	 */
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
