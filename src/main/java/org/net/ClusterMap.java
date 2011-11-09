package org.net;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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

	private static class GetRequest implements Serializable {
		private static final long serialVersionUID = 1l;
		private Object key;
	}

	private static class SetRequest implements Serializable {
		private static final long serialVersionUID = 1l;
		private Object key, value;
	}

	public ClusterMap(Cluster cluster) {
		this.cluster = cluster;

		synchronized (cluster) { // Avoid missed cluster events
			peers.addAll(cluster.getActivePeers());
			Collections.sort(peers);

			cluster.addOnJoined(onJoined);
			cluster.addOnLeft(onLeft);
			cluster.setOnReceive(GetRequest.class, onGet);
			cluster.setOnReceive(SetRequest.class, onSet);
		}
	}

	private final Setter<String> onJoined = new Setter<String>() {
		public Void perform(String peer) {
			synchronized (ClusterMap.this) {
				peers.add(peer);
				Collections.sort(peers);
				return null;
			}
		}
	};

	private final Setter<String> onLeft = new Setter<String>() {
		public Void perform(String peer) {
			synchronized (ClusterMap.this) {
				peers.remove(peer);
				Collections.sort(peers);
				return null;
			}
		}
	};

	private final Transformer<GetRequest, V> onGet = new Transformer<GetRequest, V>() {
		public V perform(GetRequest request) {
			return localMap.get(request.key);
		}
	};

	private final Transformer<SetRequest, V> onSet = new Transformer<SetRequest, V>() {
		public V perform(SetRequest request) {
			@SuppressWarnings("unchecked")
			K key = (K) request.key;
			@SuppressWarnings("unchecked")
			V value = (V) request.value;
			return localMap.put(key, value);
		}
	};

	public V get(K key) {
		return getFromPeer(getPeerByHash(key), key);
	}

	public V set(K key, V value) {
		return setToPeer(getPeerByHash(key), key, value);
	}

	private V getFromPeer(String peer, K key) {
		GetRequest request = new GetRequest();
		request.key = key;
		return requestForResponse(peer, request);
	}

	private V setToPeer(String peer, K key, V value) {
		SetRequest request = new SetRequest();
		request.key = key;
		request.value = value;
		return requestForResponse(peer, request);
	}

	private V requestForResponse(String peer, Serializable request) {
		@SuppressWarnings("unchecked")
		V value = (V) cluster.requestForResponse(peer, request);
		return value;
	}

	private String getPeerByHash(K key) {
		int hash = Util.hashCode(key);
		return peers.get(hash % peers.size());
	}

}
