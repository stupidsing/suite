package suite.net;

import static suite.util.Friends.rethrow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import suite.primitive.Bytes;
import suite.util.Fail;

public class NetUtil {

	public static int bytesToInt(Bytes bytes) {
		int value = 0, i = 4;
		do
			value = value << 8 | bytes.get(--i) & 0xFF;
		while (0 < i);
		return value;
	}

	public static Bytes intToBytes(int value) {
		var bytes = new byte[4];
		for (var i = 0; i < 4; i++) {
			bytes[i] = (byte) (value & 0xFF);
			value >>>= 8;
		}
		return Bytes.of(bytes);
	}

	public static Bytes serialize(Object o) {
		var baos = new ByteArrayOutputStream();
		return rethrow(() -> {
			var out = new ObjectOutputStream(baos);
			out.writeObject(o);
			out.flush();
			out.close();
			return Bytes.of(baos.toByteArray());
		});
	}

	public static <T> T deserialize(Bytes s) {
		try (var bais = new ByteArrayInputStream(s.toArray()); var in = new ObjectInputStream(bais);) {
			@SuppressWarnings("unchecked")
			var t = (T) in.readObject();
			return t;
		} catch (ClassNotFoundException | IOException ex) {
			return Fail.t(ex);
		}
	}

}
