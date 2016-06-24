package suite.net;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import suite.net.channels.RequestResponseChannel;
import suite.primitive.Bytes;
import suite.util.Util;

public class RequestResponseMatcher {

	private static AtomicInteger tokenCounter = new AtomicInteger();

	// TODO clean-up lost requests
	private Map<Integer, Bytes[]> requests = new HashMap<>();

	public Bytes requestForResponse(RequestResponseChannel channel, Bytes request) {
		return requestForResponse(channel, request, 0);
	}

	public Bytes requestForResponse(RequestResponseChannel channel, Bytes request, int timeOut) {
		Integer token = tokenCounter.getAndIncrement();
		Bytes holder[] = new Bytes[1];

		synchronized (holder) {
			requests.put(token, holder);
			channel.send(RequestResponseChannel.REQUEST, token, request);

			while (holder[0] == null)
				Util.wait(holder, timeOut);

			requests.remove(token);
		}

		return holder[0];
	}

	public void onResponseReceived(int token, Bytes respond) {
		Bytes holder[] = requests.get(token);

		if (holder != null)
			synchronized (holder) {
				holder[0] = respond;
				holder.notify();
			}
	}

}
