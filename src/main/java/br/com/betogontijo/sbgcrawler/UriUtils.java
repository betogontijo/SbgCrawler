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
		if (path.startsWith("file://") || path.startsWith("http://") || path.startsWith("https://")) {
			uri = new URI(path);
		} else if (new File(path).exists()) {
			path = path.replace("\\", "/");
			uri = new URI("file://" + path);
		} else {
			HttpURLConnection huc = (HttpURLConnection) new URL("http://" + path).openConnection();
			huc.setRequestMethod("HEAD");
			if (huc.getResponseCode() == HttpURLConnection.HTTP_OK) {
				uri = new URI(huc.getURL().toString());
			} else {
				huc = (HttpURLConnection) new URL("https://" + path).openConnection();
				huc.setRequestMethod("HEAD");
				if (huc.getResponseCode() == HttpURLConnection.HTTP_OK) {
					uri = new URI(huc.getURL().toString());
				}
			}
		}
		return uri;
	}

	public static String getIndex(String uriPath) throws URISyntaxException {
		URI uri = new URI(uriPath);
		StringBuffer sb = new StringBuffer();
		if (uri.getScheme() != null) {
			sb.append(uri.getScheme());
			sb.append(':');
		}
		if (uri.isOpaque()) {
			sb.append(uri.getSchemeSpecificPart());
		} else {
			if (uri.getHost() != null) {
				sb.append("//");
				if (uri.getUserInfo() != null) {
					sb.append(uri.getUserInfo());
					sb.append('@');
				}
				boolean needBrackets = ((uri.getHost().indexOf(':') >= 0) && !uri.getHost().startsWith("[")
						&& !uri.getHost().endsWith("]"));
				if (needBrackets)
					sb.append('[');
				sb.append(uri.getHost());
				if (needBrackets)
					sb.append(']');
				if (uri.getPort() != -1) {
					sb.append(':');
					sb.append(uri.getPort());
				}
			}
		}
		String string = sb.toString();
		return string.endsWith("/") ? string : string + "/";
	}

	public static String formatUri(String uri) throws URISyntaxException {
		return new URI(uri).toString();
	}
}
