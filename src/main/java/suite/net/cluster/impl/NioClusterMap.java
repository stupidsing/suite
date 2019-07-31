package suite.net.cluster.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import suite.net.cluster.impl.ClusterMapUtil.GetQuery;
import suite.net.cluster.impl.ClusterMapUtil.PutQuery;

public class NioClusterMap<K, V> {

	private NioCluster cluster;
	private List<String> peers = new ArrayList<>();
	private Map<K, V> localMap = new HashMap<>();

	public NioClusterMap(NioCluster cluster) {
		synchronized (this.cluster = cluster) { // avoid missed cluster events
			peers.addAll(cluster.getActivePeers());
			Collections.sort(peers);

			cluster.getOnJoined().wire(this, onJoined);
			cluster.getOnLeft().wire(this, onLeft);
			cluster.setOnReceive(GetQuery.Request.class, onGet);
			cluster.setOnReceive(PutQuery.Request.class, onPut);
		}
	}

	private Sink<String> onJoined = peer -> onChangePeers(() -> peers.add(peer));
	private Sink<String> onLeft = peer -> onChangePeers(() -> peers.remove(peer));

	private void onChangePeers(Runnable runnable) {
		synchronized (NioClusterMap.this) {
			runnable.run();
			Collections.sort(peers);
		}
	}

	private Fun<GetQuery.Request, GetQuery.Response> onGet = request -> {
		var response = new GetQuery.Response();
		response.value = localMap.get(request.key);
		return response;
	};

	private Fun<PutQuery.Request, PutQuery.Response> onPut = request -> {
		@SuppressWarnings("unchecked")
		var key = (K) request.key;
		@SuppressWarnings("unchecked")
		var value = (V) request.value;

		var response = new PutQuery.Response();
		response.value = localMap.put(key, value);
		return response;
	};

	public void get(K key, Sink<V> okay, Sink<IOException> fail) {
		getFromPeer(getPeerByHash(key), key, okay, fail);
	}

	public void set(K key, V value, Sink<V> okay, Sink<IOException> fail) {
		putToPeer(getPeerByHash(key), key, value, okay, fail);
	}

	private void getFromPeer(String peer, K key, Sink<V> okay, Sink<IOException> fail) {
		var request = new GetQuery.Request();
		request.key = key;

		requestForResponse(peer, request, object -> {
			var response = (GetQuery.Response) object;

			@SuppressWarnings("unchecked")
			var value = (V) response.value;
			okay.f(value);
		}, fail);
	}

	private void putToPeer(String peer, K key, V value, Sink<V> okay, Sink<IOException> fail) {
		var request = new PutQuery.Request();
		request.key = key;
		request.value = value;

		requestForResponse(peer, request, object -> {
			var response = (PutQuery.Response) object;

			@SuppressWarnings("unchecked")
			var value1 = (V) response.value;
			okay.f(value1);
		}, fail);
	}

	private void requestForResponse(String peer, Serializable request, Sink<Serializable> okay, Sink<IOException> fail) {
		cluster.requestForResponse(peer, request, object -> okay.f((Serializable) object), fail);
	}

	private String getPeerByHash(K key) {
		var hash = Objects.hashCode(key);
		return peers.get(hash % peers.size());
	}

}
