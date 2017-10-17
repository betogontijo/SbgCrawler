package br.com.betogontijo.sbgcrawler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Service;

import br.com.betogontijo.sbgbeans.crawler.documents.Domain;
import br.com.betogontijo.sbgbeans.crawler.documents.SbgDocument;
import br.com.betogontijo.sbgbeans.crawler.repositories.DomainRepository;
import br.com.betogontijo.sbgbeans.crawler.repositories.SbgDocumentRepository;

//@SpringBootApplication
//public class SbgBeansApplication {
//	public static void main(String[] args) {
//		SpringApplication.run(SbgBeansApplication.class, args);
//	}
//
//	@Bean
//	CommandLineRunner init(NodeRepository domainRepository) {
//		return args -> {
//			Node obj = domainRepository.findByPrefix("batata");
//			System.out.println(obj);
//
//			InvertedList invertedList = new InvertedList();
//			List<Integer> docRefList = new ArrayList<Integer>();
//			docRefList.add(1);
//			invertedList.setDocRefList(docRefList);
//			List<int[]> occurrencesList = new ArrayList<int[]>();
//			occurrencesList.add(new int[1]);
//			invertedList.setOccurrencesList(occurrencesList);
//
//			int n = domainRepository.upsertNode("batata", invertedList, new HashMap<Character, Node>());
//			System.out.println("Number of records updated : " + n);
//		};
//
//	}
//}

@Service
@ComponentScan({ "br.com.betogontijo.sbgbeans.crawler" })
@EnableMongoRepositories("br.com.betogontijo.sbgbeans.crawler.repositories")
public class SbgDataSource {

	static final String INSERT_REFERENCE_QUERY = "INSERT INTO refs (uri) VALUES ";
	static final String SELECT_AND_REMOVE_REFERENCE_QUERY = "DELETE FROM refs LIMIT ? RETURNING uri";

	private AtomicLong documentIdCounter = new AtomicLong();
	private AtomicLong domainIdCounter = new AtomicLong();

	private ConnectionManager connectionManager;

	private Connection mariaDbConnection;

	private Queue<String> referencesBufferQueue;

	private static int bufferSize;

	private static int threads;

	@Autowired
	MongoTemplate mongoTemplate;

	@Autowired
	SbgDocumentRepository documentRepository;

	@Autowired
	DomainRepository domainRepository;

	{
		try {
			// Starts connectionFactory
			connectionManager = new ConnectionManager();

			// Get the connection for references database
			setMariaDbConnection(connectionManager.getReferencesConnection());

			// Get last document and domain ID
			Long documentCount = 0L;
			try {
				documentCount = documentRepository.count();
			} catch (NullPointerException e) {
			}
			documentIdCounter.set(documentCount);
			Long domainCount = 0L;
			try {
				domainCount = domainRepository.count();
			} catch (NullPointerException e) {
			}
			domainIdCounter.set(domainCount);

			setReferencesBufferQueue(new ConcurrentSetQueue<String>());
			Properties properties = new Properties();
			properties.load(ClassLoader.getSystemResourceAsStream("sbgcrawler.properties"));
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
	public Long getDocIdCounter() {
		return documentIdCounter.get();
	}

	/**
	 * @param document
	 * @param nextDocument
	 */
	public void updateDomainsDb(Domain domain) {
		domainRepository.upsertDomain(domain.getUri(), domain.getRobotsContent());
	}

	/**
	 * @param document
	 * @param nextDocument
	 */
	public void updateDocumentsDb(SbgDocument document) {
		documentRepository.upsertDocument(document.getUri(), document.getLastModified(), document.getBody());
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
		if (documentIdCounter.get() > getBufferSize() && getReferencesBufferQueue().size() < getBufferPerThread()) {
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
