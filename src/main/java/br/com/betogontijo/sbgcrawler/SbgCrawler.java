package br.com.betogontijo.sbgcrawler;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bson.types.Binary;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author BETO
 *
 */
public class SbgCrawler implements Runnable {

	private SbgDataSource dataSource = SbgDataSource.getInstance();
	private static final Binary ROBOTS_NULL = new Binary(new byte[1]);

	/**
	 * @param uri
	 * @throws Exception
	 */
	public void crawl(String uri) throws Exception {
		// Query to stored domain
		Domain domainQuery = new Domain(Domain.getDomain(uri));
		// Load or create the domain for this document
		Domain domain = dataSource.findDomain(domainQuery);

		// Try to retrieve robots.txt from this domain
		if (domain.getRobotsContent() == null && domain.getRobotsContent() != ROBOTS_NULL) {
			Binary robotsCotent = getRobotsCotent(domain.getUri());
			if (robotsCotent == null) {
				domain.setRobotsContent(ROBOTS_NULL);
			} else {
				domain.setRobotsContent(robotsCotent);
			}
		}

		// Check if page is allowed
		if (!domain.isPageAllowed(uri)) {
			return;
		}

		// Query to stored document
		SbgDocument sbgDocumentQuery = new SbgDocument(uri);
		// Load if the domain was loaded from db.
		// Otherwise create it.
		// If the domain wasnt loaded we can assure that document wasnt either.
		SbgDocument sbgDocument = dataSource.findDocument(sbgDocumentQuery, domain.isLoadedInstance());
		// Check if still updated
		if (sbgDocument.isOutDated()) {
			return;
		}
		try {
			// Retrieve the HTML
			org.jsoup.nodes.Document doc = Jsoup.parse(sbgDocument.getInputStream(), null, sbgDocument.getUri());
			sbgDocument.setContent(doc.text());
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
			dataSource.updateDomainsDb(domainQuery, domain);
			dataSource.updateDocumentsDb(sbgDocumentQuery, sbgDocument);
		} catch (Exception e1) {
		}
	}

	/**
	 * @param domainUrl
	 * @return
	 */
	private Binary getRobotsCotent(String domainUrl) {
		try {
			URL url = new URL("http://" + domainUrl + "/robots.txt");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			return new Binary(IOUtils.toByteArray(connection.getInputStream()));
		} catch (Exception e) {
			return null;
		}
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
