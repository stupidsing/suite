package suite.net;

import static suite.util.Friends.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import suite.primitive.Bytes;

public class NetUtil {

	public static int bytesToInt(Bytes bytes) {
		return bsToInt(bytes.toArray());
	}

	public static int bsToInt(byte[] bs) {
		int value = 0, i = 4;
		do
			value = value << 8 | bs[--i] & 0xFF;
		while (0 < i);
		return value;
	}

	public static Bytes intToBytes(int value) {
		return Bytes.of(intToBs(value));
	}

	public static byte[] intToBs(int value) {
		var bytes = new byte[4];
		for (var i = 0; i < 4; i++) {
			bytes[i] = (byte) (value & 0xFF);
			value >>>= 8;
		}
		return bytes;
	}

	public static Bytes serialize(Object object) {
		var baos = new ByteArrayOutputStream();
		try (var baos_ = new ByteArrayOutputStream(); var out = new ObjectOutputStream(baos_);) {
			out.writeObject(object);
		} catch (IOException ex) {
			return fail(ex);
		}
		return Bytes.of(baos.toByteArray());
	}

	public static <T> T deserialize(Bytes bytes) {
		try (var bais = new ByteArrayInputStream(bytes.toArray()); var in = new ObjectInputStream(bais);) {
			@SuppressWarnings("unchecked")
			var t = (T) in.readObject();
			return t;
		} catch (ClassNotFoundException | IOException ex) {
			return fail(ex);
		}
	}

}
