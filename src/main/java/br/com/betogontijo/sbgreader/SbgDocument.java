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
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SbgDocument extends SbgMap<String, Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8962925156333193044L;

	// String path;
	// Date lastModified;
	// ChangeFrenquency changeFrequency;
	// String body;

	/**
	 * @param uri
	 */
	public SbgDocument(String uri) {
		init(uri);
	}

	/**
	 * @param map
	 * @param uri
	 */
	public SbgDocument(Map<String, Object> map, String uri) {
		if (map != null) {
			for (Entry<String, Object> entry : map.entrySet()) {
				put(entry.getKey(), entry.getValue());
			}
		} else {
			init(uri);
		}
	}

	/**
	 * @param uri
	 */
	private void init(String uri) {
		setPath(uri);
	}

	/**
	 * @return
	 */
	public boolean isOutDated() {
		// TODO make some logic to how long should a document be updated
		return getLastModified() != null;
	}

	/**
	 * @return
	 */
	public String getPath() {
		return (String) get("path");
	}

	/**
	 * @param path
	 */
	public void setPath(String path) {
		put("path", path);
	}

	/**
	 * @return
	 */
	public Long getLastModified() {
		return (Long) get("lastModified");
	}

	/**
	 * @param lastModified
	 */
	public void setLastModified(long lastModified) {
		put("lastModified", lastModified);
	}

	// Create a map of words with their positions
	/**
	 * @param body
	 */
	public void setContent(String body) {
		Map<String, List<Integer>> wordsPos = new SbgMap<String, List<Integer>>();
		Scanner in = new Scanner(body);
		int pos = 0;
		while (in.hasNext()) {
			String word = removeAccents(in.next());
			if (!word.isEmpty()) {
				if (wordsPos.get(word) != null) {
					wordsPos.get(word).add(pos++);
				} else {
					List<Integer> positions = new ArrayList<Integer>();
					positions.add(pos++);
					wordsPos.put(word, positions);
				}
			}
		}
		in.close();
		put("wordsPos", wordsPos);
	}

	// get input stream to this document
	/**
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
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

	// Check if this document has this word (deprecated)
	/**
	 * @param query
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public int containsWord(String query) {
		Map<String, List<Integer>> wordsPos = (Map<String, List<Integer>>) get("wordsPos");
		String[] words = query.split(" ");
		List<Integer> positions = wordsPos.get(words[0]);
		int queryCount = 0;
		if (positions != null) {
			for (Integer integer : positions) {
				queryCount += contaisWord(wordsPos, 1, words, integer);
			}
		}
		return queryCount;
	}

	/**
	 * @param wordsPos
	 * @param currentPos
	 * @param words
	 * @param documentPos
	 * @return
	 */
	public int contaisWord(Map<String, List<Integer>> wordsPos, int currentPos, String[] words, int documentPos) {
		if (currentPos == words.length) {
			return 1;
		} else {
			int matches = 0;
			List<Integer> positions = wordsPos.get(words[currentPos]);
			if (positions != null) {
				currentPos++;
				for (Integer integer : positions) {
					if (integer - documentPos == 1) {
						matches += contaisWord(wordsPos, currentPos, words, integer);
					}
				}
			}
			return matches;
		}
	}

	/**
	 * @param string
	 * @return
	 */
	public static String removeAccents(String string) {
		if (string != null) {
			string = string.toLowerCase();
			string = Normalizer.normalize(string, Normalizer.Form.NFD);
			string = string.replaceAll("[^a-z0-9\\s]", "");
		}
		return string;
	}
}
