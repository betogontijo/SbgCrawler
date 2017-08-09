package br.com.betogontijo.sbgreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

class WebDigger {
	private Map<String, WebSite> webSitesMap;
	File webCache = new File("web.dat");

	WebDigger() {
		loadCache();
	}

	@SuppressWarnings("unchecked")
	private void loadCache() {
		try {
			FileInputStream fileInputStream = new FileInputStream(webCache);
			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

			webSitesMap = (HashMap<String, WebSite>) objectInputStream.readObject();
			objectInputStream.close();
		} catch (Exception e) {
			webSitesMap = new HashMap<String, WebSite>();
		}
	}

	public void breadthFirstSearch(String url, int cont, int i) {
		if (cont > i) {
			WebSite webSite = webSitesMap.get(url);
			if (webSite != null) {
				if (!webSite.isReacheable() || (webSite.getReferences() != null && !webSite.isOutDated())) {
					return;
				}
			} else {
				webSite = new WebSite(url);
			}
			webSite.setReferences(webSitesMap);
			List<WebSite> references = webSite.getReferences();
			Queue<String> fila = new LinkedList<String>();
			System.out.print("Level " + i + ": " + url + " -> ");
			for (int j = 0; j < references.size(); j++) {
				String visiting = references.get(j).getUrl();
				System.out.print(references.get(j).getUrl() + "; ");
				fila.add(visiting);
			}
			if (fila.isEmpty()) {
				System.out.print("Nao ha referencias.");
			}
			System.out.println();
			i++;
			while (!fila.isEmpty()) {
				String next = fila.remove();
				breadthFirstSearch(next, cont, i);
			}
		}
	}

	public void breadthFirstSearch(String url) {
		WebSite webSite = webSitesMap.get(url);
		if (webSite != null) {
			if (!webSite.isReacheable() || (webSite.getReferences() != null && !webSite.isOutDated())) {
				return;
			}
		} else {
			webSite = new WebSite(url);
		}
		webSite.setReferences(webSitesMap);
		List<WebSite> references = webSite.getReferences();
		Queue<String> fila = new LinkedList<String>();
		System.out.print(url + " -> ");
		for (int j = 0; j < references.size(); j++) {
			String visiting = references.get(j).getUrl();
			System.out.print(references.get(j).getUrl() + "; ");
			fila.add(visiting);
		}
		if (fila.isEmpty()) {
			System.out.print("Nao ha referencias.");
		}
		System.out.println();
		while (!fila.isEmpty()) {
			String next = fila.remove();
			breadthFirstSearch(next);
		}
	}

	public List<WebSite> getRanking() {
		List<WebSite> ranking = new ArrayList<WebSite>(webSitesMap.values());
		Collections.sort(ranking, new Comparator<WebSite>() {
			public int compare(WebSite o1, WebSite o2) {
				return (int) (o2.getPointedByCount() - o1.getPointedByCount());
			}
		});
		return ranking;
	}

	public void saveCache() {
		FileOutputStream fileOutputStream;
		try {
			fileOutputStream = new FileOutputStream(webCache);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

			objectOutputStream.writeObject(webSitesMap);
			objectOutputStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
