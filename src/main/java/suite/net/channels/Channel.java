package suite.net.channels;

import java.io.IOException;

import suite.primitive.Bytes;

public interface Channel {

	public interface Sender {
		public Bytes apply(Bytes i) throws IOException;
	}

	public void onConnected(Sender sender);

	public void onClose();

	public void onReceive(Bytes message);

	public void onTrySend() throws IOException;

}
