package br.com.betogontijo.sbgreader;

import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

class SbgCrawler implements Closeable {

	MongoClient mongoClient;

	@SuppressWarnings("rawtypes")
	MongoCollection<Map> domainDB;

	@SuppressWarnings("rawtypes")
	MongoCollection<Map> documentDB;

	private Integer docIdCounter = 1;

	static final String DOMAINS_COLLECTION_NAME = "domain";
	static final String DOCUMENTS_COLLECTION_NAME = "document";

	SbgCrawler() {
		loadCache();
	}

	@SuppressWarnings("unchecked")
	private void loadCache() {
		mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase database = mongoClient.getDatabase("SbgDB");
		domainDB = database.getCollection(DOMAINS_COLLECTION_NAME, Map.class);
		documentDB = database.getCollection(DOCUMENTS_COLLECTION_NAME, Map.class);
		BasicDBObject id = new BasicDBObject();
		id.put("_id", -1);
		Map<String, Object> maxId = documentDB.find().sort(id).first();
		if (maxId != null) {
			setDocIdCounter((Integer) maxId.get("_id"));
		}
	}

	public void crawl(String uri, String referedBy) throws IOException {
		Domain domainQuery = new Domain(Domain.getDomain(uri));
		Domain domain = findDomain(domainQuery);
		if (domain.getRobotsContent() == null) {
			domain.setRobotsContent(getRobotsCotent(domain.getUri()));
		}
		if (!domain.isPageAllowed(uri)) {
			return;
		}
		SbgDocument sbgDocumentQuery = new SbgDocument(uri);
		SbgDocument sbgDocument = findPage(sbgDocumentQuery);
		if (sbgDocument.isProcessed()) {
			return;
		}
		try {
			sbgDocument.setLastModified(new Date());
			if (referedBy != null) {
				domain.increaseRank(referedBy);
			}
			org.jsoup.nodes.Document doc = Jsoup.parse(IOUtils.toString(sbgDocument.getInputStream()));
			sbgDocument.setContent(doc.text());
			Elements links = doc.select("[href]").not("[href~=(?i)\\.(png|jpe?g|css|gif|ico|js|json|mov)]")
					.not("[hreflang]");
			Queue<String> references = new LinkedList<String>();
			for (Element element : links) {
				String href = element.attr("abs:href").split("\\?")[0];
				try {
					href = UriUtils.pathToUri(href).toString();
					references.add(href);
				} catch (Exception e) {
				}
			}
			updateDB(domainDB, domainQuery, domain);
			updateDB(documentDB, sbgDocumentQuery, sbgDocument);

			if (references.isEmpty()) {
//				System.out.println(uri + " -> Nao ha referencias.");
			} else {
//				System.out.println(uri + " -> " + references + ".");
				while (!references.isEmpty()) {
					try {
						crawl(references.remove(), uri);
					} catch (Exception e) {
						// Holds the exception, so the entire machine doesnt
						// stop.
					}
				}
			}

		} catch (URISyntaxException e1) {
			// Just ignore this exception?
		}
	}

	@SuppressWarnings("unchecked")
	private Domain findDomain(Domain domain) {
		Map<String, Object> domainMap = domainDB.find(domain, SbgMap.class).first();
		Domain nextDomain = new Domain(domainMap, domain.getUri());
		return nextDomain;
	}

	@SuppressWarnings("unchecked")
	private SbgDocument findPage(SbgDocument sbgPage) {
		Map<String, Object> sbgPageMap = documentDB.find(sbgPage, SbgMap.class).first();
		SbgDocument nextSbgPage = new SbgDocument(sbgPageMap, sbgPage.getPath());
		return nextSbgPage;
	}

	@SuppressWarnings("rawtypes")
	private void updateDB(MongoCollection<Map> collection, SbgMap<String, Object> document,
			SbgMap<String, Object> nextDocument) {
		if (nextDocument.get("_id") == null) {
			nextDocument.put("_id", docIdCounter++);
			collection.insertOne(nextDocument);
		} else {
			collection.replaceOne(document, nextDocument);
		}
	}

	private byte[] getRobotsCotent(String domainUrl) {
		try {
			URL url = new URL("http://" + domainUrl + "/robots.txt");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			return IOUtils.toByteArray(connection.getInputStream());
		} catch (Exception e) {
			return null;
		}
	}

	public void close() {
		mongoClient.close();
	}

	public Integer getDocIdCounter() {
		return docIdCounter;
	}

	private void setDocIdCounter(Integer docIdCounter) {
		this.docIdCounter = docIdCounter;
	}
}
