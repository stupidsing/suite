package suite.net.cluster;

import java.util.Set;

import suite.net.Service;
import suite.streamlet.Nerve;

public interface ClusterProbe extends Service {

	public boolean isActive(String node);

	public Set<String> getActivePeers();

	public Nerve<String> getOnJoined();

	public Nerve<String> getOnLeft();

}
