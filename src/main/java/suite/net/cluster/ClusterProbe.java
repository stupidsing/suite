package suite.net.cluster;

import java.util.Set;

import suite.util.FunUtil.Sink;

public interface ClusterProbe {

	public void start();

	public void stop();

	public boolean isActive(String node);

	public String dumpActivePeers();

	public Set<String> getActivePeers();

	public void setMe(String me);

	public void setOnJoined(Sink<String> onJoined);

	public void setOnLeft(Sink<String> onLeft);

}
