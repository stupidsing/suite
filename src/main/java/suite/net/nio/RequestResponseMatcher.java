package suite.net.nio;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import suite.net.nio.NioChannelFactory.RequestResponseNioChannel;
import suite.node.util.Mutable;
import suite.primitive.Bytes;
import suite.util.Util;

public class RequestResponseMatcher {

	private static AtomicInteger tokenCounter = new AtomicInteger();

	// TODO clean-up lost requests
	private Map<Integer, Mutable<Bytes>> requests = new HashMap<>();

	public Bytes requestForResponse(RequestResponseNioChannel channel, Bytes request) {
		return requestForResponse(channel, request, 0);
	}

	public Bytes requestForResponse(RequestResponseNioChannel channel, Bytes request, int timeOut) {
		Integer token = tokenCounter.getAndIncrement();
		Mutable<Bytes> holder = Mutable.nil();

		synchronized (holder) {
			requests.put(token, holder);
			channel.send(RequestResponseNioChannel.REQUEST, token, request);

			while (holder.get() == null)
				Util.wait(holder, timeOut);

			requests.remove(token);
		}

		return holder.get();
	}

	public void onResponseReceived(int token, Bytes respond) {
		Mutable<Bytes> holder = requests.get(token);

		if (holder != null)
			synchronized (holder) {
				holder.set(respond);
				holder.notify();
			}
	}

}
