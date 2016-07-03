package suite.net.nio;

import suite.primitive.Bytes;
import suite.streamlet.Reactive;
import suite.util.FunUtil.Fun;

public class NioChannel {

	public final Reactive<Fun<Bytes, Bytes>> onConnected = new Reactive<>();
	public final Reactive<Boolean> onClose = new Reactive<>();
	public final Reactive<Bytes> onReceive = new Reactive<>();
	public final Reactive<Boolean> onTrySend = new Reactive<>();

}
