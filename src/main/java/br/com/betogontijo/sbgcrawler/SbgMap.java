package br.com.betogontijo.sbgcrawler;

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
	/**
	 * 
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
	/**
	 * @param key
	 * @param value
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
	/**
	 * @param map
	 */
	public SbgMap(final Map<K, V> map) {
		this.map = new LinkedHashMap<K, V>(map);
	}

	/* (non-Javadoc)
	 * @see org.bson.conversions.Bson#toBsonDocument(java.lang.Class, org.bson.codecs.configuration.CodecRegistry)
	 */
	@SuppressWarnings("rawtypes")
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> documentClass, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<Map>(this, codecRegistry.get(Map.class));
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#clear()
	 */
	@Override
	public void clear() {
		map.clear();
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#entrySet()
	 */
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#get(java.lang.Object)
	 */
	@Override
	public V get(Object key) {
		return map.get(key);
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#keySet()
	 */
	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public V put(K key, V value) {
		return map.put(key, value);
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#putAll(java.util.Map)
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		map.putAll(m);
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#remove(java.lang.Object)
	 */
	@Override
	public V remove(Object key) {
		return map.remove(key);
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#size()
	 */
	@Override
	public int size() {
		return map.size();
	}

	/* (non-Javadoc)
	 * @see java.util.HashMap#values()
	 */
	@Override
	public Collection<V> values() {
		return map.values();
	}

}
