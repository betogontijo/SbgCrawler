package br.com.betogontijo.sbgreader;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebSite implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6846758160796514506L;
	private String url;
	private List<WebSite> references;
	private long pointedBy;
	private long timeStamp;
	private boolean reacheable = true;
	private String body;

	WebSite(String url) {
		this.url = url;
		this.pointedBy = 0;
		timeStamp = System.currentTimeMillis();

	}

	public String getUrl() {
		return this.url;
	}

	public List<WebSite> getReferences() {
		return this.references;
	}

	public void setReferences(Map<String, WebSite> webSitesMap) {
		try {
			this.references = new ArrayList<WebSite>();
			Document doc = Jsoup.connect(url).get();
			this.setBody(doc.text());
			Elements links = doc.select("[href]:not([href~=(?i)\\.(png|jpe?g|css|gif|ico|js|json)])");
			for (Element element : links) {
				String href = element.attr("abs:href").split("\\?")[0];
				try {
					new URL(href);
				} catch (Exception e) {
					continue;
				}
				WebSite reference = webSitesMap.get(href);
				if (reference == null) {
					reference = new WebSite(href);
				}
				references.add(reference);
				webSitesMap.put(href, reference);
				reference.increasePointedBy(url);
			}
		} catch (Exception e) {
			// Nao foi possivel conectar
			setReacheable(false);
		}
	}

	public void increasePointedBy(String url) {
		if (!getDomainName(this.url).equals(getDomainName(url))) {
			this.pointedBy++;
		}
	}

	public static String getDomainName(String url) {
		URL uri;
		try {
			uri = new URL(url);
			String domain = uri.getHost();
			return domain.startsWith("www.") ? domain.substring(4) : domain;
		} catch (MalformedURLException e) {
			return url;
		}
	}

	public long getPointedByCount() {
		return this.pointedBy;
	}

	public long getTimeStamp() {
		return this.timeStamp;
	}

	// TODO Review
	public boolean isOutDated() {
		long validation = (System.currentTimeMillis() - getTimeStamp()) * (this.pointedBy + 1) / Integer.MAX_VALUE;
		if (validation > 1000) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return getUrl() + " := " + getPointedByCount();
	}

	public boolean isReacheable() {
		return reacheable;
	}

	public void setReacheable(boolean reacheable) {
		this.reacheable = reacheable;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}
