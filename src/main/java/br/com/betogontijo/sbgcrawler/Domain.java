package br.com.betogontijo.sbgcrawler;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

import org.bson.types.Binary;

import crawlercommons.robots.SimpleRobotRulesParser;

/**
 * @author BETO
 *
 */
public class Domain extends SbgMap<String, Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8003080503822166581L;

	// String uri;
	// List<String> referredBy;
	// byte[] robotsContent;
	private boolean loadedInstance = false;

	/**
	 * @param uri
	 */
	public Domain(String uri) {
		init(uri);
	}

	/**
	 * @param map
	 * @param uri
	 */
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

	/**
	 * @param uri
	 */
	private void init(String uri) {
		setUri(uri);
	}

	/**
	 * @return
	 */
	public String getUri() {
		return (String) get("uri");
	}

	/**
	 * @param uri
	 */
	public void setUri(String uri) {
		put("uri", uri);
	}

	// Filter uri domain
	/**
	 * @param path
	 * @return
	 * @throws MalformedURLException
	 */
	public static String getDomain(String path) throws MalformedURLException {
		try {
			URI uri = new URI(path);
			String domain = uri.getHost();
			return domain.startsWith("www.") ? domain.substring(4) : domain;
		} catch (Exception e) {
			return path;
		}
	}

	/**
	 * @param robotsCotent
	 */
	public void setRobotsContent(Binary robotsCotent) {
		put("robots", robotsCotent);
	}

	/**
	 * @return
	 */
	public Binary getRobotsContent() {
		try {
			return (Binary) get("robots");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @param page
	 * @return
	 */
	public boolean isPageAllowed(String page) {
		return new SimpleRobotRulesParser()
				.parseContent(getUri(), getRobotsContent().getData(), "text/html; charset=UTF-8", "SbgRobot")
				.isAllowed(page);
	}

	/**
	 * @return
	 */
	public boolean isLoadedInstance() {
		return loadedInstance;
	}

	/**
	 * @param loadedInstance
	 */
	public void setLoadedInstance(boolean loadedInstance) {
		this.loadedInstance = loadedInstance;
	}
}
