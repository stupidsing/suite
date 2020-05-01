package suite.concurrent;

import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class EventBus {

	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

	private Map<String, Fun<Object, Object>> handlers = new ConcurrentHashMap<>();

	public Closeable handle(String eb, Fun<Object, Object> fun) {
		handlers.put(eb, fun);
		return () -> handlers.remove(eb);
	}

	public Fut<Object> rr(String eb, Object request) {
		var handler = handlers.get(eb);

		return Fut.of(fut -> executor.execute(() -> {
			try {
				fut.complete(handler.apply(request));
			} catch (Exception ex) {
				fut.error(ex);
			}
		}));
	}

	public void publish(String eb, Object request) {
		var handler = handlers.get(eb);
		executor.execute(() -> handler.apply(request));
	}

	public void subscribe(String eb, Sink<Object> sink) {
		handlers.put(eb, request -> {
			sink.f(request);
			return true;
		});
	}

}
