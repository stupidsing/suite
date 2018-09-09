package suite.net.nio;

import java.util.HashMap;
import java.util.Map;

import suite.adt.Mutable;
import suite.adt.pair.Pair;
import suite.concurrent.Condition;
import suite.net.nio.NioplexFactory.RequestResponseNioplex;
import suite.primitive.Bytes;
import suite.primitive.IntPrimitives.IntSink;
import suite.util.Util;

public class RequestResponseMatcher {

	// tODO clean-up lost requests
	private Map<Integer, Pair<Mutable<Bytes>, Condition>> requests = new HashMap<>();

	public Bytes requestForResponse(RequestResponseNioplex channel, Bytes request) {
		return requestForResponse(token -> channel.send(RequestResponseNioplex.REQUEST, token, request));
	}

	public Bytes requestForResponse(IntSink sink) {
		return requestForResponse(sink, Long.MAX_VALUE);
	}

	public Bytes requestForResponse(IntSink sink, long timeout) {
		var token = Util.temp();
		var holder = Mutable.<Bytes> nil();
		var condition = new Condition();

		return condition.waitThen(() -> {
			return holder.value() != null;
		}, () -> {
			requests.put(token, Pair.of(holder, condition));
			sink.sink(token);
		}, () -> {
			requests.remove(token);
			return holder.value();
		}, timeout);
	}

	public void onResponseReceived(int token, Bytes response) {
		requests.get(token).map((holder, condition) -> {
			condition.satisfyOne(() -> holder.set(response));
			return null;
		});
	}

}
