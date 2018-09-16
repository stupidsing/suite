package suite.net.cluster;

import java.util.Set;

import suite.streamlet.Signal;

public interface ClusterProbe {

	public void start();

	public void stop();

	public boolean isActive(String node);

	public Set<String> getActivePeers();

	public Signal<String> getOnJoined();

	public Signal<String> getOnLeft();

}
