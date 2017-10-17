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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.betogontijo.sbgbeans.crawler.documents.Domain;
import br.com.betogontijo.sbgbeans.crawler.documents.SbgDocument;
import crawlercommons.robots.SimpleRobotRulesParser;

/**
 * @author BETO
 *
 */
@Service
public class SbgCrawler implements Runnable {

	@Autowired
	private SbgDataSource dataSource;
	
	private static final byte[] ROBOTS_NULL = new byte[1];

	/**
	 * @param uri
	 * @throws Exception
	 */
	public void crawl(String uri) throws Exception {
		// Load or create the domain for this document
		Domain domain = dataSource.findDomain(getDomain(uri));
		if (domain == null) {
			domain = new Domain();
			domain.setUri(getDomain(uri));
		}

		// Try to retrieve robots.txt from this domain
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
		SbgDocument sbgDocument = dataSource.findDocument(uri);
		if (sbgDocument == null) {
			sbgDocument = new SbgDocument();
			sbgDocument.setUri(uri);
		}
		// Check if still updated
		if (sbgDocument.isOutDated()) {
			return;
		}
		try {
			// Retrieve the HTML
			org.jsoup.nodes.Document doc = Jsoup.parse(getInputStream(sbgDocument.getUri()), null,
					sbgDocument.getUri());
			sbgDocument.setBody(doc.text());
			sbgDocument.setLastModified(System.currentTimeMillis());

			// Filter references
			Elements links = doc.select("[href]").not("[href~=(?i)\\.(png|jpe?g|css|gif|ico|js|json|mov)]")
					.not("[hreflang]");
			List<String> references = new ArrayList<String>();
			for (Element element : links) {
				String href = element.attr("abs:href").split("\\?")[0];
				try {
					// Parse full path reference uri
					references.add(UriUtils.pathToUri(href).toString());
					// getReferences().add(href);
				} catch (Exception e) {
				}
			}
			dataSource.insertReference(references);
			// Update the db
			dataSource.updateDomainsDb(domain);
			dataSource.updateDocumentsDb(sbgDocument);
		} catch (Exception e1) {
		}
	}

	private String getDomain(String path) {
		try {
			URI uri = new URI(path);
			String domain = uri.getHost();
			return domain.startsWith("www.") ? domain.substring(4) : domain;
		} catch (Exception e) {
			return path;
		}
	}

	public static InputStream getInputStream(String uriPath)
			throws MalformedURLException, IOException, URISyntaxException {
		URI uri = new URI(uriPath);
		String scheme = uri.getScheme();

		if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("ftp")) {
			return uri.toURL().openStream();
		} else if (scheme.equalsIgnoreCase("file")) {
			return new FileInputStream(new File(uri.getPath()));
		} else {
			return null;
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
			e.printStackTrace();
		}
	}

}
