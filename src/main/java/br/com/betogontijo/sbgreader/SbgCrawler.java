package br.com.betogontijo.sbgreader;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class SbgCrawler implements Runnable {

	SbgDataSource dataSource = SbgDataSource.getInstance();

	public void crawl(String uri) throws Exception {
		// Query to stored domain
		Domain domainQuery = new Domain(Domain.getDomain(uri));
		// Load or create the domain for this document
		Domain domain = dataSource.findDomain(domainQuery);

		// Try to retrieve robots.txt from this domain
		if (domain.getRobotsContent() == null) {
			domain.setRobotsContent(getRobotsCotent(domain.getUri()));
		}

		// Check if page is allowed
		if (!domain.isPageAllowed(uri)) {
			return;
		}

		// Query to stored document
		SbgDocument sbgDocumentQuery = new SbgDocument(uri);
		// Load or create the document
		SbgDocument sbgDocument = dataSource.findDocument(sbgDocumentQuery, domain.isLoadedInstance());
		// Check if still updated
		if (sbgDocument.isOutDated()) {
			return;
		}
		try {
			// Retrieve the HTML
			org.jsoup.nodes.Document doc = Jsoup.parse(IOUtils.toString(sbgDocument.getInputStream()));
			sbgDocument.setContent(doc.text());
			sbgDocument.setLastModified(System.currentTimeMillis());

			// Filter references
			Elements links = doc.select("[href]").not("[href~=(?i)\\.(png|jpe?g|css|gif|ico|js|json|mov)]")
					.not("[hreflang]");
			for (Element element : links) {
				String href = element.attr("abs:href").split("\\?")[0];
				try {
					// Parse full path reference uri
					href = UriUtils.pathToUri(href).toString();
					dataSource.insertReference(href);
					// getReferences().add(href);
				} catch (Exception e) {
				}
			}
			// Update the db
			dataSource.updateDomainsDb(domainQuery, domain);
			dataSource.updateDocumentsDb(sbgDocumentQuery, sbgDocument);
		} catch (Exception e1) {
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

	public void run() {
		try {
			crawl(dataSource.getReference());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
