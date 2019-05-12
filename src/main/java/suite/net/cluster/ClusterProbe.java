package suite.net.cluster;

import java.util.Set;

import suite.streamlet.Pusher;

public interface ClusterProbe {

	public void start();

	public void stop();

	public boolean isActive(String node);

	public Set<String> getActivePeers();

	public Pusher<String> getOnJoined();

	public Pusher<String> getOnLeft();

}
