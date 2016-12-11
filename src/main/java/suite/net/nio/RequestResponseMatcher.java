package suite.net.nio;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import suite.adt.Pair;
import suite.concurrent.Condition;
import suite.net.nio.NioChannelFactory.RequestResponseNioChannel;
import suite.node.util.Mutable;
import suite.primitive.Bytes;

public class RequestResponseMatcher {

	private static AtomicInteger tokenCounter = new AtomicInteger();

	// tODO clean-up lost requests
	private Map<Integer, Pair<Mutable<Bytes>, Condition>> requests = new HashMap<>();

	public Bytes requestForResponse(RequestResponseNioChannel channel, Bytes request) {
		return requestForResponse(channel, request, 0);
	}

	public Bytes requestForResponse(RequestResponseNioChannel channel, Bytes request, int timeOut) {
		Integer token = tokenCounter.getAndIncrement();
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
		Pair<Mutable<Bytes>, Condition> pair = requests.get(token);
		Mutable<Bytes> holder = pair.t0;
		Condition condition = pair.t1;
		condition.thenNotify(() -> holder.set(response));
	}

}
