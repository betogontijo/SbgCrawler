package br.com.betogontijo.sbgcrawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.betogontijo.sbgbeans.crawler.documents.Domain;
import br.com.betogontijo.sbgbeans.crawler.documents.SbgDocument;
import crawlercommons.robots.SimpleRobotRulesParser;

/**
 * @author BETO
 *
 */
public class SbgCrawler implements Runnable {

	private SbgCrawlerDao dataSource;

	private static final byte[] ROBOTS_NULL = new byte[1];

	SbgCrawler(SbgCrawlerDao dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * @param uri
	 * @throws Exception
	 */
	public void crawl(String uri) throws Exception {
		// Load or create the domain for this document
		boolean insertDomain = false;
		String domainUri = UriUtils.getDomain(uri);
		Domain domain = dataSource.findDomain(domainUri);
		if (domain == null) {
			insertDomain = true;
			domain = new Domain();
			domain.setUri(domainUri);
		}

		// Try to retrieve robots.txt from this domain

		if (domain.getRobotsContent() == ROBOTS_NULL) {
			System.out.println();
		}

		if (domain.getRobotsContent() == null && domain.getRobotsContent() != ROBOTS_NULL) {
			byte[] robotsCotent = getRobotsCotent(domain.getUri());
			if (robotsCotent == null) {
				domain.setRobotsContent(ROBOTS_NULL);
			} else {
				domain.setRobotsContent(robotsCotent);
			}
		}

		// Check if page is allowed
		if (!isPageAllowed(domain, uri)) {
			return;
		}
		// Load if the domain was loaded from db.
		// Otherwise create it.
		// If the domain wasnt loaded we can assure that document wasnt either.
		boolean insertDocument = false;
		SbgDocument sbgDocument = dataSource.findDocument(uri);
		if (sbgDocument == null) {
			insertDocument = true;
			sbgDocument = new SbgDocument();
			sbgDocument.setUri(uri);
		}
		// Check if still updated
		if (!sbgDocument.isOutDated()) {
			return;
		}
		try {
			// Retrieve the HTML
			org.jsoup.nodes.Document doc = Jsoup.parse(getInputStream(sbgDocument.getUri()), "UTF-8",
					sbgDocument.getUri());
			sbgDocument.setBody(doc.text());
			sbgDocument.setLastModified(System.currentTimeMillis());

			// Filter references
			Elements links = doc.select("[href]").not("[href~=(?i)\\.(png|jpe?g|css|gif|ico|js|json|mov)]")
					.not("[hreflang]").not("[href^=mailto]");
			List<String> references = new ArrayList<String>();
			for (Element element : links) {
				try {
					String href = new URI(element.attr("abs:href")).toString();
					if (!href.isEmpty()) {
						// Parse full path reference uri
						references.add(href);
					}
				} catch (Exception e) {
				}
			}
			dataSource.insertReference(references);
			// Update the db
			dataSource.updateDomainsDb(domain, insertDomain);
			dataSource.updateDocumentsDb(sbgDocument, insertDocument);
		} catch (Exception e1) {
		}
	}

	public static InputStream getInputStream(String uriPath)
			throws MalformedURLException, IOException, URISyntaxException {
		URI uri = new URI(uriPath);
		String scheme = uri.getScheme();

		if (scheme.equalsIgnoreCase("file")) {
			return new FileInputStream(new File(uri.getPath()));
		} else {
			try {
				return uri.toURL().openStream();
			} catch (Exception e) {
				return null;
			}
		}
	}

	/**
	 * @param domainUrl
	 * @return
	 */
	private byte[] getRobotsCotent(String domainUrl) {
		try {
			URL url = new URL("http://" + domainUrl + "/robots.txt");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			return IOUtils.toByteArray(connection.getInputStream());
		} catch (Exception e) {
			return null;
		}
	}

	public boolean isPageAllowed(Domain domain, String page) {
		return new SimpleRobotRulesParser()
				.parseContent(domain.getUri(), domain.getRobotsContent(), "text/html; charset=UTF-8", "SbgRobot")
				.isAllowed(page);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			crawl(dataSource.getReference());
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

}
