package br.com.betogontijo.sbgcrawler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import br.com.betogontijo.sbgbeans.crawler.documents.SbgDocument;
import br.com.betogontijo.sbgbeans.crawler.repositories.SbgDocumentRepository;

public class SbgCrawlerDao {

	static final String INSERT_REFERENCE_QUERY = "INSERT IGNORE INTO refs (uri) VALUES ";
	static final String SELECT_AND_REMOVE_REFERENCE_QUERY = "DELETE FROM refs LIMIT ? RETURNING uri";

	private AtomicInteger documentIdCounter;

	private Connection mariaDbConnection;

	private ConcurrentSetQueue<String> referencesBufferQueue = new ConcurrentSetQueue<String>();

	private static int bufferSize;

	private static int bufferPerThread;

	SbgDocumentRepository documentRepository;

	static SbgCrawlerDao dataSource;

	public SbgCrawlerDao(int threadNumber, int bufferSize, SbgDocumentRepository documentRepository) throws Exception {
		this.documentRepository = documentRepository;

		// Get the connection for references database
		initiateMariaDB();

		// Get last document and domain ID
		int documentCount = (int) documentRepository.count();
		documentIdCounter = new AtomicInteger(documentCount);

		setBufferSize(bufferSize);
		setBufferPerThread(bufferSize / threadNumber);
	}

	private void initiateMariaDB() throws Exception {
		Properties properties = new Properties();
		properties.load(ClassLoader.getSystemResourceAsStream("sbgcrawler.properties"));
		// Get database connection driver
		String driver = properties.getProperty("maridb.driver");
		Class.forName(driver);

		// Configure connection parameters
		String url = properties.getProperty("maridb.url");
		String username = properties.getProperty("maridb.username");
		String password = properties.getProperty("maridb.password");

		// Try connection
		mariaDbConnection = DriverManager.getConnection(url, username, password);
	}

	/**
	 * @return
	 */
	public ConcurrentSetQueue<String> getReferencesBufferQueue() {
		return referencesBufferQueue;
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
		SbgCrawlerDao.bufferSize = bufferSize;
	}

	/**
	 * @return
	 */
	public int getDocIdCounter() {
		return documentIdCounter.get();
	}

	/**
	 * @param document
	 * @param nextDocument
	 */
	public void updateDocumentsDb(SbgDocument document, boolean insertDocument) {
		if (insertDocument) {
			document.setId(documentIdCounter.getAndIncrement());
			documentRepository.insertDocument(document);
		} else {
			documentRepository.updateDocument(document);
		}
	}

	/**
	 * @param sbgPage
	 * @param search
	 * @return
	 */
	public SbgDocument findDocument(String uri) {
		return documentRepository.findByUri(uri);
	}

	/**
	 * @param reference
	 */
	public void insertReference(List<String> reference) {
		getReferencesBufferQueue().addAll((reference));
		if (getReferencesBufferQueue().size() > getBufferSize()) {
			int n = getBufferPerThread();
			try {
				Statement createStatement = mariaDbConnection.createStatement();
				while (n > 0) {
					StringBuilder builder = new StringBuilder(INSERT_REFERENCE_QUERY);
					for (int i = 1024000; builder.length() < i && n > 0; n--) {
						String remove = getReferencesBufferQueue().remove().replaceAll("\'", "\'\'");
						if (remove != null) {
							builder.append("('");
							builder.append(remove);
							builder.append("'),");
						} else {
							break;
						}
					}
					builder.deleteCharAt(builder.length() - 1);
					createStatement.addBatch(builder.toString());
				}
				createStatement.executeBatch();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return
	 */
	public String getReference() {
		// When reaches size of the buffer is time to fill it again
		if (documentIdCounter.get() > getBufferSize() && getReferencesBufferQueue().size() < getBufferPerThread()) {
			try {
				PreparedStatement prepareStatement = mariaDbConnection
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

	public int getBufferPerThread() {
		return bufferPerThread;
	}

	public void setBufferPerThread(int bufferPerThread) {
		SbgCrawlerDao.bufferPerThread = bufferPerThread;
	}

}
