package suite.http;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import suite.http.HttpSessionController.Session;
import suite.http.HttpSessionController.SessionManager;

public class HttpSessionManager implements SessionManager {

	private Map<String, Session> sessions = new ConcurrentHashMap<>();
	private AtomicBoolean isCleaning = new AtomicBoolean(false);

	@Override
	public Session get(String id) {
		return sessions.get(id);
	}

	@Override
	public void put(String id, Session session) {
		int size0 = sessions.size();
		sessions.put(id, session);
		int size1 = sessions.size();

		if (lg2(size1) > lg2(size0)) // Exceeded a power of two?
			if (isCleaning.getAndSet(true)) { // One thread cleaning is enough
				long current = System.currentTimeMillis();
				Iterator<Session> iter = sessions.values().iterator();

				while (iter.hasNext())
					if (current > iter.next().getLastRequestDt() + HttpSessionController.TIMEOUTDURATION)
						iter.remove();

				isCleaning.set(false);
			}
	}

	private int lg2(int n) {
		int i = 0;
		while (n > 0) {
			n /= 2;
			i++;
		}
		return i;
	}

}
