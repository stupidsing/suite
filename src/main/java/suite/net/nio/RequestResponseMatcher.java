package suite.net.nio;

import java.util.HashMap;
import java.util.Map;

import suite.adt.Mutable;
import suite.adt.pair.Pair;
import suite.concurrent.Condition;
import suite.net.nio.NioChannelFactory.RequestResponseNioChannel;
import suite.primitive.Bytes;
import suite.util.Util;

public class RequestResponseMatcher {

	// tODO clean-up lost requests
	private Map<Integer, Pair<Mutable<Bytes>, Condition>> requests = new HashMap<>();

	public Bytes requestForResponse(RequestResponseNioChannel channel, Bytes request) {
		return requestForResponse(channel, request, 0);
	}

	public Bytes requestForResponse(RequestResponseNioChannel channel, Bytes request, int timeOut) {
		var token = Util.temp();
		Mutable<Bytes> holder = Mutable.nil();
		Condition condition = new Condition(() -> holder.get() != null);

		return condition.waitThen(() -> {
			requests.put(token, Pair.of(holder, condition));
			channel.send(RequestResponseNioChannel.REQUEST, token, request);
		}, () -> {
			requests.remove(token);
			return holder.get();
		});
	}

	public void onResponseReceived(int token, Bytes response) {
		var pair = requests.get(token);
		Mutable<Bytes> holder = pair.t0;
		var condition = pair.t1;
		condition.thenNotify(() -> holder.set(response));
	}

}
