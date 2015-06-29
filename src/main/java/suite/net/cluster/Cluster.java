package suite.net.cluster;

import java.util.Set;

import suite.net.Service;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

public interface Cluster extends Service {

	public static class ClusterException extends RuntimeException {
		private static final long serialVersionUID = 1;

		public ClusterException(Throwable cause) {
			super(cause);
		}
	}

	public Object requestForResponse(String peer, Object request);

	public void addOnJoined(Sink<String> onJoined);

	public void addOnLeft(Sink<String> onLeft);

	public <I, O> void setOnReceive(Class<I> clazz, Fun<I, O> onReceive);

	public String getMe();

	public Set<String> getActivePeers();

}
