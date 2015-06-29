package suite.net.cluster;

import java.util.Set;

import suite.net.Service;
import suite.util.FunUtil.Sink;

public interface ClusterProbe extends Service {

	public boolean isActive(String node);

	public String dumpActivePeers();

	public Set<String> getActivePeers();

	public void setMe(String me);

	public void setOnJoined(Sink<String> onJoined);

	public void setOnLeft(Sink<String> onLeft);

}
