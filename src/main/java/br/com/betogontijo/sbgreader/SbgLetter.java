package br.com.betogontijo.sbgreader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBObject;

public class SbgLetter {

	char letter;
	List<String> pages;
	Map<Character, SbgLetter> nextLetters;

	SbgLetter(char letter) {
		this.letter = letter;
		pages = new ArrayList<String>();
		nextLetters = new HashMap<Character, SbgLetter>();
	}

	char getLetter() {
		return this.letter;
	}

	boolean addPage(String page) {
		return pages.add(page);
	}

	public List<String> getPages() {
		return this.pages;
	}

	List<SbgLetter> getNextLetters() {
		return (List<SbgLetter>) nextLetters.values();
	}

	public boolean addNext(SbgLetter sbgLetter) {
		try {
			nextLetters.put(sbgLetter.getLetter(), sbgLetter);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public Object getPage(String key) {
		return nextLetters.get(key);
	}

	public BasicDBObject getBasicObject() {
		BasicDBObject basicObject = new BasicDBObject();
		basicObject.put("letter", letter);
		basicObject.put("pages", pages);
		basicObject.put("nextLetters", nextLetters);
		return basicObject;
	}

}
