package suite.net.channels;

import java.io.IOException;

import suite.os.LogUtil;
import suite.primitive.Bytes;

/**
 * Channel with a send buffer.
 */
public abstract class BufferedChannel implements Channel {

	private Sender sender;
	private Bytes toSend = Bytes.empty;

	public void send(Bytes message) {
		toSend = toSend.append(message);

		try {
			onTrySend();
		} catch (IOException ex) {
			LogUtil.error(ex);
		}
	}

	@Override
	public void onConnected(Sender sender) {
		this.sender = sender;
	}

	@Override
	public void onClose() {
	}

	@Override
	public void onTrySend() throws IOException {
		toSend = sender.apply(toSend);
	}

}
