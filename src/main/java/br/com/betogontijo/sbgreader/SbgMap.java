package br.com.betogontijo.sbgreader;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

public class SbgMap<K, V> extends HashMap<K, V> implements Map<K, V>, Serializable, Bson {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3654902012086479217L;
	private final LinkedHashMap<K, V> map;

	/**
	 * Creates an empty Document instance.
	 */
	public SbgMap() {
		map = new LinkedHashMap<K, V>();
	}

	/**
	 * Create a Document instance initialized with the given key/value pair.
	 *
	 * @param key
	 *            key
	 * @param value
	 *            value
	 */
	public SbgMap(final K key, final V value) {
		map = new LinkedHashMap<K, V>();
		map.put(key, value);
	}

	/**
	 * Creates a Document instance initialized with the given map.
	 *
	 * @param map
	 *            initial map
	 */
	public SbgMap(final Map<K, V> map) {
		this.map = new LinkedHashMap<K, V>(map);
	}

	@SuppressWarnings("rawtypes")
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> documentClass, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<Map>(this, codecRegistry.get(Map.class));
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	@Override
	public V get(Object key) {
		return map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public V put(K key, V value) {
		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		map.putAll(m);
	}

	@Override
	public V remove(Object key) {
		return map.remove(key);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

}
