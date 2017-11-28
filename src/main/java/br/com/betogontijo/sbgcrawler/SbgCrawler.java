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
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import br.com.betogontijo.sbgbeans.crawler.documents.SbgDocument;
import crawlercommons.robots.SimpleRobotRulesParser;

/**
 * @author BETO
 *
 */
public class SbgCrawler implements Runnable {

	private SbgCrawlerDao dataSource;

	private boolean canceled;

	CountDownLatch latch;

	SbgCrawler(SbgCrawlerDao dataSource, CountDownLatch latch) {
		this.dataSource = dataSource;
		setCanceled(false);
		this.latch = latch;
	}

	/**
	 * @param uri
	 * @throws Exception
	 */
	public void crawl(String uri) throws Exception {
		// Load or create the domain for this document
		boolean insertIndex = false;
		String uriIndex = UriUtils.getIndex(uri);
		SbgDocument index = dataSource.findDocument(uriIndex);
		if (index == null) {
			insertIndex = true;
			index = new SbgDocument();
			index.setUri(uriIndex);

			// Try to retrieve robots.txt from this domain
			index.setRobotsContent(getRobotsCotent(uriIndex));

			// Check if page is allowed
			if (!isPageAllowed(index, uri)) {
				return;
			}
		} else
		// Try to retrieve robots.txt from this domain
		if ((index.getRobotsContent() != null)
				// Check if page is allowed
				&& (!isPageAllowed(index, uri))) {
			return;
		}

		// Load if the domain was loaded from db.
		// Otherwise create it.
		// If the domain wasnt loaded we can assure that document wasnt either.
		boolean insertDocument = false;
		SbgDocument sbgDocument = null;
		if (!insertIndex) {
			sbgDocument = dataSource.findDocument(uri);
		}
		if (sbgDocument == null) {
			insertDocument = true;
			sbgDocument = new SbgDocument();
			sbgDocument.setUri(uri);
		}
		// Check if still updated
		if (!sbgDocument.isOutDated()) {
			return;
		}
		fetchUri(sbgDocument);
		// Update the db
		if (!(index.getUri().equals(sbgDocument.getUri())) && insertIndex) {
			fetchUri(index);
			dataSource.insertIndexDocumentsDb(index);
		}
		dataSource.updateDocumentsDb(sbgDocument, insertDocument);
	}

	private void fetchUri(SbgDocument sbgDocument) throws IOException, MalformedURLException, URISyntaxException {
		try {
			// Retrieve the HTML
			InputStream inputStream = getInputStream(sbgDocument.getUri());
			Document doc = Jsoup.parse(inputStream, null, sbgDocument.getUri());
			// TODO Check 200 OK
			sbgDocument.setTitle(doc.title());
			sbgDocument.setBody(doc.text());
			sbgDocument.setLastModified(System.currentTimeMillis());

			// Filter references
			// Elements links =
			// doc.select("[href]").not("[href~=(?i)\\.(png|jpe?g|css|gif|ico|js|json|mov)]")
			// .not("[hreflang]").not("[href^=mailto]");
			// List<String> references = new ArrayList<String>();
			// for (Element element : links) {
			// try {
			// String href = UriUtils.formatUri(element.attr("abs:href"));
			// if (!href.isEmpty()) {
			// // Parse full path reference uri
			// references.add(href);
			// }
			// } catch (URISyntaxException e) {
			// }
			// }
			// dataSource.insertReference(references);
		} catch (Exception e) {

		}
	}

	public static InputStream getInputStream(String uriPath)
			throws MalformedURLException, IOException, URISyntaxException {
		// URI uri = new URI(uriPath);
		URI uri = new URI("file://" + uriPath);
		String scheme = uri.getScheme();

		if (scheme.equalsIgnoreCase("file")) {
			return new FileInputStream(new File(uri.getSchemeSpecificPart()));
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

	public boolean isPageAllowed(SbgDocument index, String page) {
		return new SimpleRobotRulesParser()
				.parseContent(index.getUri(), index.getRobotsContent(), "text/html; charset=UTF-8", "SbgRobot")
				.isAllowed(page);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		String reference = null;
		while ((reference = dataSource.getReference()) != null && !isCanceled()) {
			try {
				crawl(reference);
			} catch (Exception e) {
			}
		}
		latch.countDown();
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

}
