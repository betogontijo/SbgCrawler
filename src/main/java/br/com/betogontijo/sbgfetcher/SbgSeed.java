package br.com.betogontijo.sbgfetcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class SbgSeed {

	URI path;

	SbgSeed(String seedPath) {
		try {
			path = new URI(seedPath);
		} catch (URISyntaxException e) {
			try {
				if (new File("file://" + seedPath).exists()) {
					path = new URI("file://" + seedPath);
				} else {
					path = new URI("http://" + seedPath);
				}
			} catch (URISyntaxException e1) {

			}
		}
	}

	OutputStream getOutputStream() throws MalformedURLException, IOException {
		String scheme = path.getScheme();

		if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
			return path.toURL().openConnection().getOutputStream();
		}
		// else if(scheme.equalsIgnoreCase("ftp")){}
		else if (scheme.equalsIgnoreCase("file")) {
			return new FileOutputStream(Paths.get(path).toFile());
		} else {
			return null;
		}
	}

	InputStream getInputStream() throws MalformedURLException, IOException {
		String scheme = path.getScheme();

		if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("ftp")) {
			return path.toURL().openStream();
		} else if (scheme.equalsIgnoreCase("file")) {
			return new FileInputStream(new File(path.getPath()));
		} else {
			return null;
		}
	}

	Document getDocument() {
		try {
			return Jsoup.parse(IOUtils.toString(getInputStream()));
		} catch (Exception e) {
			return null;
		}
	}

}
