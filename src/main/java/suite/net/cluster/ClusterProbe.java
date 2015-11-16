package suite.net.cluster;

import java.util.Set;

import suite.net.Service;
import suite.streamlet.Reactive;

public interface ClusterProbe extends Service {

	public boolean isActive(String node);

	public Set<String> getActivePeers();

	public Reactive<String> getOnJoined();

	public Reactive<String> getOnLeft();

}
