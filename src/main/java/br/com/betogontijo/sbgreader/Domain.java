package br.com.betogontijo.sbgreader;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
	// List<SbgPage> pages;

	public Domain(String uri) {
		setUri(uri);
		setReferedBy(new ArrayList<String>());
		setPages(new ArrayList<SbgPage>());
	}

	public Domain(String uri, boolean isSearch) {
		setUri(uri);
		if (!isSearch) {
			setReferedBy(new ArrayList<String>());
			setPages(new ArrayList<SbgPage>());
		}
	}

	public Domain(Map<String, Object> map) {
		for (Entry<String, Object> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	public String getUri() {
		return (String) get("uri");
	}

	public void setUri(String uri) {
		put("uri", uri);
	}

	@SuppressWarnings("unchecked")
	public List<String> getReferedBy() {
		return ((List<String>) get("referedBy"));
	}

	public void setReferedBy(List<String> referedBy) {
		put("referedBy", referedBy);
	}

	@SuppressWarnings("unchecked")
	public List<SbgPage> getPages() {
		return ((List<SbgPage>) get("pages"));
	}

	public void setPages(List<SbgPage> pages) {
		put("pages", pages);
	}

	// @SuppressWarnings("unchecked")
	// public Domain(Map<String, Object> domain) {
	// this.map = domain;
	// this.url = (String) domain.get("url");
	// this.referedBy = (ArrayList<String>) domain.get("referedBy");
	// if (referedBy != null) {
	// this.referedBy = new ArrayList<String>();
	// }
	// this.pages = (List<String>) domain.get("pages");
	// if (pages == null) {
	// this.pages = new ArrayList<String>();
	// }
	// }

	public int getRank() {
		return getReferedBy().size();
	}

	//
	public boolean increaseRank(String url) {
		if (!getReferedBy().contains(url)) {
			try {
				return getReferedBy().add(getDomain(url));
			} catch (MalformedURLException e) {
				return false;
			}
		}
		return false;
	}

	public static String getDomain(String path) throws MalformedURLException {
		try {
			URI uri = new URI(path);
			String domain = uri.getHost();
			return domain.startsWith("www.") ? domain.substring(4) : domain;
		} catch (Exception e) {
			return path;
		}
	}

	public boolean addPage(SbgPage page) {
		List<SbgPage> listSbgPages = getPages();
		if (listSbgPages == null) {
			setPages(new ArrayList<SbgPage>());
			return getPages().add(page);
		} else {
			if (listSbgPages.contains(page)) {
				return false;
			} else {
				return listSbgPages.add(page);
			}
		}
	}

	public void setRobotsContent(byte[] robotsCotent) {
		put("robots", robotsCotent);
	}

	public byte[] getRobotsContent() {
		return ((Binary) get("robots")).getData();
	}

	public boolean isPageAllowed(String page) {
		return new SimpleRobotRulesParser()
				.parseContent(getUri(), getRobotsContent(), "text/html; charset=UTF-8", "SbgRobot").isAllowed(page);
	}
}
