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
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

class SbgCrawler implements Closeable {
	MongoClient mongoClient;
	@SuppressWarnings("rawtypes")
	MongoCollection<Map> domainDB;
	@SuppressWarnings("rawtypes")
	MongoCollection<Map> pageDB;

	SbgCrawler() {
		loadCache();
	}

	private void loadCache() {
		mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase database = mongoClient.getDatabase("SbgDB");
		domainDB = database.getCollection("domain", Map.class);
		pageDB = database.getCollection("page", Map.class);
	}

	@SuppressWarnings("unchecked")
	public void crawl(String page) throws IOException {
		try {
			page = UriUtils.pathToUri(page).toString();
		} catch (URISyntaxException e2) {
			return;
		}
		String domainUri = Domain.getDomain(page);
		Domain domain = new Domain(domainUri, true);
		Map<String, Object> domainMap = domainDB.find(domain, SbgMap.class).first();
		Domain nextDomain = null;
		if (domainMap != null) {
			domainMap.remove("_id");
			nextDomain = new Domain(domainMap);
		}
		if (nextDomain == null) {
			nextDomain = new Domain(domainUri);
			nextDomain.setRobotsContent(getRobotsCotent(domainUri));
		}
		if (!nextDomain.isPageAllowed(page)) {
			return;
		}
		SbgPage sbgPage = new SbgPage(page);
		Map<String, Object> sbgPageMap = pageDB.find(sbgPage, SbgMap.class).first();
		SbgPage nextSbgPage = null;
		if (sbgPageMap != null) {
			sbgPageMap.remove("_id");
			nextSbgPage = new SbgPage(sbgPageMap);
			nextSbgPage.setDomain(nextDomain);
			if (!nextSbgPage.isOutDated()) {
				return;
			}
		} else {
			nextSbgPage = sbgPage;
			nextSbgPage.setLastModified(new Date());

		}
		nextDomain.addPage(nextSbgPage);
		try {
			org.jsoup.nodes.Document doc = Jsoup.parse(IOUtils.toString(nextSbgPage.getInputStream()));
			nextSbgPage.setContent(doc.text());
			Elements links = doc.select("[href]:not([href~=(?i)\\.(png|jpe?g|css|gif|ico|js|json)])");
			Queue<String> references = new LinkedList<String>();
			for (Element element : links) {
				String href = element.attr("abs:href").split("\\?")[0];
				try {
					String refDomainUrl = Domain.getDomain(href);
					Domain refDomain = new Domain(refDomainUrl, true);
					Map<String, Object> refDomainMap = domainDB.find(refDomain, SbgMap.class).first();
					Domain nextRefDomain = null;
					if (refDomainMap != null) {
						refDomainMap.remove("_id");
						nextRefDomain = new Domain(refDomainMap);
					}
					if (nextRefDomain == null) {
						nextRefDomain = new Domain(refDomainUrl);
					}
					nextRefDomain.increaseRank(page);
					if (refDomainMap == null) {
						domainDB.insertOne(nextRefDomain);
					} else {
						domainDB.replaceOne(refDomain, nextRefDomain);
					}
				} catch (Exception e) {
					continue;
				}
				references.add(href);
			}
			if (domainMap == null) {
				domainDB.insertOne(nextDomain);
			} else {
				domainDB.replaceOne(domain, nextDomain);
			}
			if (sbgPageMap == null) {
				pageDB.insertOne(nextSbgPage);
			} else {
				pageDB.replaceOne(sbgPage, nextSbgPage);
			}

			if (references.isEmpty()) {
				System.out.println(page + " -> Nao ha referencias.");
			} else {
				System.out.println(page + " -> " + references + ".");
				while (!references.isEmpty()) {
					String next = references.remove();
					try {
						crawl(next);
					} catch (Exception e) {
					}
				}
			}

		} catch (HttpStatusException e) {
			if (domainMap == null) {
				domainDB.insertOne(nextDomain);
			} else {
				domainDB.replaceOne(domain, nextDomain);
			}
			if (sbgPageMap == null) {
				pageDB.insertOne(nextSbgPage);
			} else {
				pageDB.replaceOne(sbgPage, nextSbgPage);
			}
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
	}

	private byte[] getRobotsCotent(String domainUrl) {
		try {
			URL url = new URL("http://" + domainUrl + "/robots.txt");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			return IOUtils.toByteArray(connection.getInputStream());
		} catch (Exception e) {
			return new byte[0];
		}
	}

	public void close() {
		mongoClient.close();
	}
}
