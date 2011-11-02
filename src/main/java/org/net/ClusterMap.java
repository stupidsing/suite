package org.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.util.Util;
import org.util.Util.Setter;
import org.util.Util.Transformer;

public class ClusterMap<K, V> {

	private Cluster cluster;
	private List<String> peers = new ArrayList<String>();
	private Map<K, V> localMap = new HashMap<K, V>();

	private static class GetRequest {
		private Object key;
	}

	private static class SetRequest {
		private Object key, value;
	}

	public ClusterMap(Cluster cluster) {
		this.cluster = cluster;

		cluster.addOnJoined(new Setter<String>() {
			public Void perform(String peer) {
				peers.add(peer);
				return null;
			}
		});
		cluster.addOnLeft(new Setter<String>() {
			public Void perform(String peer) {
				peers.remove(peer);
				return null;
			}
		});

		cluster.setOnReceive(GetRequest.class,
				new Transformer<GetRequest, V>() {
					public V perform(GetRequest request) {
						return localMap.get(request.key);
					}
				});
		cluster.setOnReceive(SetRequest.class,
				new Transformer<SetRequest, V>() {
					public V perform(SetRequest request) {
						@SuppressWarnings("unchecked")
						K key = (K) request.key;
						@SuppressWarnings("unchecked")
						V value = (V) request.value;
						return localMap.put(key, value);
					}
				});
	}

	public V get(K key) {
		return getFromPeer(getPeerByHash(key), key);
	}

	public V set(K key, V value) {
		return setToPeer(getPeerByHash(key), key, value);
	}

	private V getFromPeer(String peer, K key) {
		GetRequest request = new GetRequest();
		request.key = key;

		String response = cluster.requestForResponse(peer, request);
		return deserializeValue(response);
	}

	private V setToPeer(String peer, K key, V value) {
		SetRequest request = new SetRequest();
		request.key = key;
		request.value = value;

		String response = cluster.requestForResponse(peer, request);
		return deserializeValue(response);
	}

	private String getPeerByHash(K key) {
		int hash = Util.hashCode(key);
		String peer = peers.get(hash % peers.size());
		return peer;
	}

	private V deserializeValue(String m) {
		@SuppressWarnings("unchecked")
		V value = (V) NetUtil.deserialize(m);
		return value;
	}

}
