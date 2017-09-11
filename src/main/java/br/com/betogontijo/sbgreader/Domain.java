package br.com.betogontijo.sbgreader;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

import org.bson.types.Binary;

import crawlercommons.robots.SimpleRobotRulesParser;

public class Domain extends SbgMap<String, Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8003080503822166581L;

	// String uri;
	// List<String> referedBy;
	// byte[] robotsContent;
	private boolean loadedInstance = false;

	public Domain(String uri) {
		init(uri);
	}

	public Domain(Map<String, Object> map, String uri) {
		if (map != null) {
			for (Entry<String, Object> entry : map.entrySet()) {
				put(entry.getKey(), entry.getValue());
			}
			setLoadedInstance(true);
		} else {
			init(uri);
		}
	}

	private void init(String uri) {
		setUri(uri);
	}

	public String getUri() {
		return (String) get("uri");
	}

	public void setUri(String uri) {
		put("uri", uri);
	}

	// Filter uri domain
	public static String getDomain(String path) throws MalformedURLException {
		try {
			URI uri = new URI(path);
			String domain = uri.getHost();
			return domain.startsWith("www.") ? domain.substring(4) : domain;
		} catch (Exception e) {
			return path;
		}
	}

	public void setRobotsContent(byte[] robotsCotent) {
		put("robots", robotsCotent);
	}

	public byte[] getRobotsContent() {
		try {
			return ((Binary) get("robots")).getData();
		} catch (Exception e) {
			return null;
		}
	}

	public boolean isPageAllowed(String page) {
		return new SimpleRobotRulesParser()
				.parseContent(getUri(), getRobotsContent(), "text/html; charset=UTF-8", "SbgRobot").isAllowed(page);
	}

	public boolean isLoadedInstance() {
		return loadedInstance;
	}

	public void setLoadedInstance(boolean loadedInstance) {
		this.loadedInstance = loadedInstance;
	}
}
