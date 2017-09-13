package br.com.betogontijo.sbgcrawler;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public final class UriUtils {
	/**
	 * @param path
	 * @return
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static URI pathToUri(String path) throws URISyntaxException, MalformedURLException, IOException {
		URI uri = null;
		if (path.startsWith("file:") || path.startsWith("http:") || path.startsWith("https:")) {
			uri = new URI(path);
		} else if (new File(path).exists()) {
			path = path.replace("\\", "/");
			uri = new URI("file://" + path);
		} else {
			HttpURLConnection huc = (HttpURLConnection) new URL("http://" + path).openConnection();
			huc.setRequestMethod("HEAD");
			if (huc.getResponseCode() == HttpURLConnection.HTTP_OK) {
				uri = new URI(huc.getURL().toString());
			}
		}
		return uri;
	}
}
