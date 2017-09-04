package br.com.betogontijo.sbgfetcher;

import java.util.HashMap;
import java.util.Map;

public class SbgNode extends HashMap<String, Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6981076179896761064L;

	Map<String, Object> nextNode = new HashMap<String, Object>();

	SbgNode() {

	}

	SbgNode getNextNode(String node) {
		try {
			return (SbgNode) nextNode.get(node);
		} catch (Exception e) {
			return null;
		}
	}

	void putNextNode(String node, SbgNode sbgNode) {
		nextNode.put(node, sbgNode);
	}
}
