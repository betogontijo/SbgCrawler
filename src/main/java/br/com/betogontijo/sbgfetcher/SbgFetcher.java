package br.com.betogontijo.sbgfetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SbgFetcher {
	public static String getBody(String path) {
		SbgSeed sbgSeed = new SbgSeed(path);
		return sbgSeed.getDocument().text();
		// BufferedReader br = null;
		// try {
		// br = new BufferedReader(new
		// InputStreamReader(sbgSeed.getInputStream()));
		// StringBuilder body = new StringBuilder();
		// String inputLine;
		// while ((inputLine = br.readLine()) != null) {
		// body.append(inputLine);
		// }
		// return body.toString();
		// } catch (IOException e) {
		// e.printStackTrace();
		// return null;
		// } finally {
		// if (br != null) {
		// try {
		// br.close();
		// } catch (IOException e) {
		//
		// e.printStackTrace();
		// }
		// }
		// }
	}

	public static String processBody(String body) {
		return body;
		// return body.replaceAll("[^A-Za-z0-9\\s]", "").replaceAll(" ", " ");
	}

	public static void main(String[] args) {
		List<String> documents = new ArrayList<String>();
		String path = "file://C:/Users/886390/Downloads/";
		int documentsNumber = 5;
		for (int i = 1; i <= documentsNumber; i++) {
			documents.add(processBody(getBody(path + "0" + i + ".html")));
		}
		Scanner in = new Scanner(System.in);
		String query = in.nextLine();
		List<String> words = new ArrayList<String>();
		Scanner stream = new Scanner(query);
		while (stream.hasNext()) {
			String word = stream.next();
			if (word.startsWith("\"") && query.replace("\"", "").length() >= 2) {
				while (!word.endsWith("\"")) {
					word += " " + stream.next();
				}
				word = word.substring(1, word.length() - 1);
				query = query.replaceFirst("\"", "");
				query = query.replaceFirst("\"", "");
			}
			words.add(word);
		}
		List<Integer> matches = new ArrayList<Integer>();
		for (int i = 0; i < documents.size(); i++) {
			matches.add(0);
			for (int j = 0; j < words.size(); j++) {
				if (documents.get(i).contains(words.get(j))) {
					matches.set(i, matches.get(i) + 1);
				}
			}
		}
		for (int i = 0; i < documents.size(); i++) {
			System.out.println(documents.get(i) + " -> " + matches.get(i));
		}
		in.close();
		stream.close();
	}
}