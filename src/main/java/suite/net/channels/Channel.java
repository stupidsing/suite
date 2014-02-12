package suite.net.channels;

import java.io.IOException;

import suite.primitive.Bytes;
import suite.util.FunUtil.FunEx;

public interface Channel {

	public interface Sender extends FunEx<Bytes, Bytes, IOException> {
	}

	public void onConnected(Sender sender);

	public void onClose();

	public void onReceive(Bytes message);

	public void onTrySend() throws IOException;

}
