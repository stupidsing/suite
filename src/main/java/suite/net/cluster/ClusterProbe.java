package suite.net.cluster;

import suite.streamlet.Pusher;

import java.util.Set;

public interface ClusterProbe {

	public void start();

	public void stop();

	public boolean isActive(String node);

	public Set<String> getActivePeers();

	public Pusher<String> getOnJoined();

	public Pusher<String> getOnLeft();

}
