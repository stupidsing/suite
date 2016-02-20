package suite.net.channels;

import suite.net.NetUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;

/**
 * Channel that transfer data in the unit of packets.
 */
public abstract class PacketChannel extends BufferedChannel {

	private Bytes received = Bytes.empty;

	public abstract void onReceivePacket(Bytes packet);

	protected void sendPacket(Bytes packet) {
		send(new BytesBuilder() //
				.append(NetUtil.intToBytes(packet.size())) //
				.append(packet) //
				.toBytes());
	}

	@Override
	public void onReceive(Bytes message) {
		received = received.append(message);

		if (4 <= received.size()) {
			int end = 4 + NetUtil.bytesToInt(received.subbytes(0, 4));

			if (end <= received.size()) {
				Bytes packet = received.subbytes(4, end);
				received = received.subbytes(end);
				onReceivePacket(packet);
			}
		}
	}

}
