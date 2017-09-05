package br.com.betogontijo.sbgfetcher;

import java.util.Map;

import br.com.betogontijo.sbgreader.SbgMap;

public class SbgNode extends SbgMap<String, Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6981076179896761064L;

	Map<String, Object> nextNode = new SbgMap<String, Object>();

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
