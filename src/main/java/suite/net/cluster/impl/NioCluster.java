package suite.net.cluster.impl;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import suite.net.NetUtil;
import suite.net.cluster.ClusterProbe;
import suite.net.nio.NioDispatch;
import suite.object.Object_;
import suite.os.LogUtil;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.Signal;

public class NioCluster implements Closeable {

	private String me;
	private Map<String, InetSocketAddress> peers;
	private ClusterProbe probe;

	private NioDispatch nd = new NioDispatch();
	private Sink<IOException> f = LogUtil::error;
	private Closeable unlisten;

	/**
	 * Established channels connecting to peers.
	 */
	private Map<String, SocketChannel> sockChans = new HashMap<>();

	private Signal<String> onJoined;
	private Signal<String> onLeft;
	private Map<Class<?>, Fun<Object, Object>> onReceive = new HashMap<>();

	public NioCluster(String me, Map<String, InetSocketAddress> peers) throws IOException {
		this.me = me;
		this.peers = peers;
		probe = new ClusterProbeImpl(me, peers);
	}

	@Override
	public void close() throws IOException {
		nd.close();
	}

	public void start() throws IOException {
		unlisten = nd.new Responder().listen(peers.get(me).getPort(), req -> {
			var request = NetUtil.deserialize(req);
			var handler = onReceive.get(request.getClass());
			return NetUtil.serialize(handler.apply(request));
		}, f);

		onJoined = probe.getOnJoined();

		onLeft = probe.getOnLeft().map(node -> {
			var sc = sockChans.get(node);
			if (sc != null)
				nd.close(sc);
			return node;
		});

		probe.start();
	}

	public void stop() throws IOException {
		for (var sc : sockChans.values())
			nd.close(sc);

		probe.stop();
		Object_.closeQuietly(unlisten);
		nd.stop();
	}

	public void requestForResponse(String peer, Object request, Sink<Object> okay, Sink<IOException> fail) {
		if (probe.isActive(peer)) {
			var req = NetUtil.serialize(request);
			nd.new Requester(peers.get(peer)).request(req, rsp -> {
				var response = NetUtil.deserialize(rsp);
				okay.sink(response);
			});
		} else
			fail.sink(new IOException("peer " + peer + " is not active"));
	}

	public void run() {
		nd.run();
	}

	@SuppressWarnings("unchecked")
	public <I, O> void setOnReceive(Class<I> clazz, Fun<I, O> onReceive) {
		this.onReceive.put(clazz, (Fun<Object, Object>) onReceive);
	}

	public Set<String> getActivePeers() {
		return probe.getActivePeers();
	}

	public Signal<String> getOnJoined() {
		return onJoined;
	}

	public Signal<String> getOnLeft() {
		return onLeft;
	}

	public String getMe() {
		return me;
	}

}
