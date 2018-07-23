package suite.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import suite.http.HttpSessionControl.Session;
import suite.http.HttpSessionControl.SessionManager;

public class HttpSessionManager implements SessionManager {

	private Map<String, Session> sessions = new ConcurrentHashMap<>();
	private AtomicBoolean isCleaning = new AtomicBoolean(false);

	@Override
	public Session get(String id) {
		return sessions.get(id);
	}

	@Override
	public void put(String id, Session session) {
		var size0 = sessions.size();
		sessions.put(id, session);
		var size1 = sessions.size();

		if (lg2(size0) < lg2(size1)) // exceeded a power of two?
			if (isCleaning.getAndSet(true)) // one thread cleaning is enough
				try {
					var current = System.currentTimeMillis();
					var iter = sessions.values().iterator();

					while (iter.hasNext())
						if (iter.next().lastRequestDt.get() + HttpSessionControl.TIMEOUTDURATION < current)
							iter.remove();
				} finally {
					isCleaning.set(false);
				}
	}

	@Override
	public void remove(String id) {
		sessions.remove(id);
	}

	private int lg2(int n) {
		var i = 0;
		while (0 < n) {
			n /= 2;
			i++;
		}
		return i;
	}

}
