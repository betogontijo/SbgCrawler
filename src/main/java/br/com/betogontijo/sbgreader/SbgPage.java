package br.com.betogontijo.sbgreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SbgPage extends SbgMap<String, Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8962925156333193044L;

	// Domain domain;
	// String path;
	// Date lastModified;
	// ChangeFrenquency changeFrequency;
	// String body;

	SbgPage(String uri) {
		setPath(uri);
	}

	public SbgPage(Map<String, Object> map) {
		for (Entry<String, Object> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	public boolean isOutDated() {

		long validation = (new Date().getTime() - getLastModified().getTime()) * (getDomain().getRank() + 1)
				/ Integer.MAX_VALUE;
		if (validation > 1000) {
			return true;
		}
		return false;
	}

	public Domain getDomain() {
		return (Domain) get("domain");
	}

	public void setDomain(Domain domain) {
		put("domain", domain);
	}

	public String getPath() {
		return (String) get("path");
	}

	public void setPath(String path) {
		put("path", path);
	}

	public Date getLastModified() {
		return (Date) get("lastModified");
	}

	public void setLastModified(Date date) {
		put("lastModified", date);
	}

	public ChangeFrequency getChangeFrequency() {
		return (ChangeFrequency) get("changeFrequency");
	}

	public void setChangeFrequency(ChangeFrequency changeFrequency) {
		put("changeFrequency", changeFrequency);
	}

	public void setContent(String body) {
		Map<String, List<Integer>> wordsPos = new SbgMap<String, List<Integer>>();
		Scanner in = new Scanner(body);
		int pos = 0;
		while (in.hasNext()) {
			String word = removeAccents(in.next());
			if (wordsPos.get(word) != null) {
				wordsPos.get(word).add(pos++);
			} else {
				List<Integer> positions = new ArrayList<Integer>();
				positions.add(pos++);
				wordsPos.put(word, positions);
			}
		}
		in.close();
		put("wordsPos", wordsPos);
	}

	InputStream getInputStream() throws MalformedURLException, IOException, URISyntaxException {
		URI uri = new URI(getPath());
		String scheme = uri.getScheme();

		if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("ftp")) {
			return uri.toURL().openStream();
		} else if (scheme.equalsIgnoreCase("file")) {
			return new FileInputStream(new File(uri.getPath()));
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public int containsWord(String query) {
		Map<String, List<Integer>> wordsPos = (Map<String, List<Integer>>) get("wordsPos");
		String[] words = query.split(" ");
		int queryCount = 0;
		if (words.length > 1) {
			System.out.println();
			for (int i = 1; i < words.length; i++) {
				List<Integer> positions = wordsPos.get(words[i - 1]);
				if (positions != null && positions.size() > 0) {
					int k = 0;
					int j = 1;
					List<Integer> nextWordPos = wordsPos.get(words[i]);
					if (nextWordPos != null && nextWordPos.size() > 0) {
						while (j <= positions.size() && k < nextWordPos.size()) {
							if (nextWordPos != null && nextWordPos.size() > 0) {
								while (positions.get(j - 1) < nextWordPos.get(k) && j < positions.size()) {
									j++;
								}
								while (nextWordPos.get(k) < positions.get(j - 1) && k < nextWordPos.size()) {
									k++;
								}
								if (nextWordPos.get(k) - positions.get(j - 1) == 1) {
									if (i + 1 == words.length) {
										queryCount++;
										j++;
										k++;
									}
								}
							} else {
								return queryCount;
							}
						}
					} else {
						return queryCount;
					}
				} else {
					return queryCount;
				}
			}
		} else {
			if(wordsPos.get(query) != null && wordsPos.size()>0){
				queryCount++;
			}
		}
		return queryCount;
	}

	public static String removeAccents(String string) {
		if (string != null) {
			string = Normalizer.normalize(string, Normalizer.Form.NFD);
			string = string.replaceAll("[^A-Za-z0-9\\s]", "");
			// string = string.replaceAll("[^\\p{ASCII}]", "");
		}
		return string;
	}
}
