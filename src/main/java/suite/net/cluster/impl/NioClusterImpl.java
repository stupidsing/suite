//package suite.net.cluster.impl;
//
//import static suite.util.Friends.fail;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.nio.channels.SocketChannel;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set;
//
//import suite.net.NetUtil;
//import suite.net.cluster.Cluster;
//import suite.net.cluster.ClusterProbe;
//import suite.net.nio.NioDispatch;
//import suite.net.nio.NioDispatcherImpl;
//import suite.net.nio.NioplexFactory;
//import suite.net.nio.NioplexFactory.PersistentNioplex;
//import suite.net.nio.RequestResponseMatcher;
//import suite.object.Object_;
//import suite.os.LogUtil;
//import suite.primitive.Bytes;
//import suite.streamlet.FunUtil.Fun;
//import suite.streamlet.FunUtil.Sink;
//import suite.streamlet.Signal;
//import suite.util.Thread_;
//
//public class NioClusterImpl implements Cluster {
//
//	private String me;
//	private Map<String, InetSocketAddress> peers;
//	private ClusterProbe probe;
//
//	private Sink<IOException> f = LogUtil::error;
//	private NioDispatch nd = new NioDispatch();
//	private RequestResponseMatcher matcher = new RequestResponseMatcher();
//
//	/**
//	 * Established channels connecting to peers.
//	 */
//	private Map<String, SocketChannel> nioplexs = new HashMap<>();
//
//	private Signal<String> onJoined;
//	private Signal<String> onLeft;
//	private Map<Class<?>, Fun<?, ?>> onReceive = new HashMap<>();
//
//	public NioClusterImpl(String me, Map<String, InetSocketAddress> peers) throws IOException {
//		this.me = me;
//		this.peers = peers;
//
//		this.nio = new NioDispatcherImpl<>(() -> NioplexFactory.persistent( //
//				new PersistentNioplex(nio, peers.get(me)), //
//				matcher, //
//				executor, //
//				this::respondToRequest));
//
//		probe = new ClusterProbeImpl(me, peers);
//	}
//
//	@Override
//	public void start() throws IOException {
//		executor = Thread_.newExecutor();
//		nd.asyncListen(0, null, f);
//		unlisten = nio.listen(peers.get(me).getPort());
//		nio.start();
//
//		onJoined = probe.getOnJoined();
//
//		onLeft = probe.getOnLeft().map(node -> {
//			var sc = nioplexs.get(node);
//			if (sc != null)
//				nd.close(sc);
//			return node;
//		});
//
//		probe.start();
//	}
//
//	@Override
//	public void stop() {
//		for (var sc : nioplexs.values())
//			nd.close(sc);
//
//		probe.stop();
//		nd.stop();
//		Object_.closeQuietly(unlisten);
//		executor.shutdown();
//	}
//
//	@Override
//	public Object requestForResponse(String peer, Object request) {
//		if (probe.isActive(peer)) {
//			var req = NetUtil.serialize(request);
//			var sc = getChannel(peer);
//			var resp = matcher.requestForResponse(token -> nd.asyncWrite(sc, req, v -> getClass(), f));
//			return NetUtil.deserialize(resp);
//		} else
//			return fail("peer " + peer + " is not active");
//	}
//
//	private SocketChannel getChannel(String peer) {
//		var channel = nioplexs.get(peer);
//
//		if (channel == null || !channel.isConnected()) {
//			if (channel != null)
//				nd.close(channel);
//
//			nd.asyncConnect(peers.get(peer), sc -> nioplexs.put(peer, sc), f);
//		}
//
//		return channel;
//	}
//
//	private Bytes respondToRequest(Bytes req) {
//		var request = NetUtil.deserialize(req);
//		@SuppressWarnings("unchecked")
//		var handler = (Fun<Object, Object>) onReceive.get(request.getClass());
//		return NetUtil.serialize(handler.apply(request));
//	}
//
//	@Override
//	public <I, O> void setOnReceive(Class<I> clazz, Fun<I, O> onReceive) {
//		this.onReceive.put(clazz, onReceive);
//	}
//
//	@Override
//	public Set<String> getActivePeers() {
//		return probe.getActivePeers();
//	}
//
//	@Override
//	public Signal<String> getOnJoined() {
//		return onJoined;
//	}
//
//	@Override
//	public Signal<String> getOnLeft() {
//		return onLeft;
//	}
//
//	@Override
//	public String getMe() {
//		return me;
//	}
//
//}
