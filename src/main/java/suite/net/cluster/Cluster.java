package suite.net.cluster;

import java.util.Set;

import suite.net.Service;
import suite.streamlet.Nerve;
import suite.util.FunUtil.Fun;

public interface Cluster extends Service {

	public static class ClusterException extends RuntimeException {
		private static final long serialVersionUID = 1;

		public ClusterException(Throwable cause) {
			super(cause);
		}
	}

	public Object requestForResponse(String peer, Object request);

	public <I, O> void setOnReceive(Class<I> clazz, Fun<I, O> onReceive);

	public Set<String> getActivePeers();

	public Nerve<String> getOnJoined();

	public Nerve<String> getOnLeft();

	public String getMe();

}
