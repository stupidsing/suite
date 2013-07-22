package suite.net.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;

import suite.net.Bytes;
import suite.net.NioDispatcher;
import suite.net.RequestResponseMatcher;
import suite.util.FunUtil.Fun;

/**
 * Channel that will reconnect if failed for any reasons.
 */
public abstract class PersistableChannel<CL extends Channel> extends RequestResponseChannel {

	private NioDispatcher<CL> dispatcher;
	private InetSocketAddress address;
	boolean isStarted;

	public PersistableChannel(NioDispatcher<CL> dispatcher //
			, RequestResponseMatcher matcher //
			, ThreadPoolExecutor executor //
			, InetSocketAddress address //
			, Fun<Bytes, Bytes> handler) {
		super(matcher, executor, handler);
		this.dispatcher = dispatcher;
		this.address = address;
	}

	public synchronized void start() {
		isStarted = true;
		reconnect();
	}

	public synchronized void stop() {
		isStarted = false;
	}

	@Override
	public void onClose() {
		reconnect();
	}

	private void reconnect() {
		if (isStarted && !isConnected())
			try {
				dispatcher.reconnect(this, address);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

}
