package suite.net.cluster;

import java.util.Set;

import suite.net.Service;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Signal;

public interface Cluster extends Service {

	public Object requestForResponse(String peer, Object request);

	public <I, O> void setOnReceive(Class<I> clazz, Fun<I, O> onReceive);

	public Set<String> getActivePeers();

	public Signal<String> getOnJoined();

	public Signal<String> getOnLeft();

	public String getMe();

}
