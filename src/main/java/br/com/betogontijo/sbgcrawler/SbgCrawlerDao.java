package br.com.betogontijo.sbgcrawler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;

import br.com.betogontijo.sbgbeans.crawler.documents.Domain;
import br.com.betogontijo.sbgbeans.crawler.documents.SbgDocument;
import br.com.betogontijo.sbgbeans.crawler.repositories.DomainRepository;
import br.com.betogontijo.sbgbeans.crawler.repositories.SbgDocumentRepository;

public class SbgCrawlerDao {

	static final String INSERT_REFERENCE_QUERY = "INSERT INTO refs (uri) VALUES ";
	static final String SELECT_AND_REMOVE_REFERENCE_QUERY = "DELETE FROM refs LIMIT ? RETURNING uri";

	private AtomicInteger documentIdCounter;

	private Connection mariaDbConnection;

	private ConcurrentSetQueue<String> referencesBufferQueue = new ConcurrentSetQueue<String>();

	private static int bufferSize;

	private static int threadNumber;

	@Autowired
	SbgDocumentRepository documentRepository;

	@Autowired
	DomainRepository domainRepository;

	static SbgCrawlerDao dataSource;

	public SbgCrawlerDao(int threadNumber, int bufferSize, SbgDocumentRepository documentRepository,
			DomainRepository domainRepository) throws Exception {
		SbgCrawlerDao.threadNumber = threadNumber;
		this.documentRepository = documentRepository;
		this.domainRepository = domainRepository;

		// Get the connection for references database
		initiateMariaDB();

		// Get last document and domain ID
		int documentCount = (int) documentRepository.count();
		documentIdCounter = new AtomicInteger(documentCount);

		Properties properties = new Properties();
		properties.load(ClassLoader.getSystemResourceAsStream("sbgcrawler.properties"));
		setBufferSize(Integer.parseInt(properties.getProperty("environment.buffer.size")));
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
	private static int getThreads() {
		return threadNumber;
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
	public void updateDomainsDb(Domain domain, boolean insertDomain) {
		if (insertDomain) {
			domainRepository.upsertDomain(domain);
		} else {
			domainRepository.updateDomain(domain);
		}
	}

	/**
	 * @param document
	 * @param nextDocument
	 */
	public void updateDocumentsDb(SbgDocument document, boolean insertDocument) {
		if (insertDocument) {
			document.setId(documentIdCounter.getAndIncrement());
			documentRepository.upsertDocument(document);
		} else {
			documentRepository.updateDocument(document);
		}
	}

	/**
	 * @param domain
	 * @return
	 */
	public Domain findDomain(String uri) {
		return domainRepository.findByUri(uri);
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
	 * @return
	 */
	private int getBufferPerThread() {
		return getBufferSize() / getThreads();
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

}
