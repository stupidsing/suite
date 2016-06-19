package suite.net.channels;

import java.io.IOException;

import suite.primitive.Bytes;
import suite.util.FunUtil.Fun;

public interface Channel {

	public void onConnected(Fun<Bytes, Bytes> sender);

	public void onClose();

	public void onReceive(Bytes message);

	public void onTrySend() throws IOException;

}
