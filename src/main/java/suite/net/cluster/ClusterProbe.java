package suite.net.cluster;

import java.util.Set;

import suite.net.Service;
import suite.streamlet.Signal;

public interface ClusterProbe extends Service {

	public boolean isActive(String node);

	public Set<String> getActivePeers();

	public Signal<String> getOnJoined();

	public Signal<String> getOnLeft();

}
