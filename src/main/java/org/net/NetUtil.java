package org.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.util.IoUtil;

public class NetUtil {

	public static int intValue(String s) {
		int value = 0, i = 4;
		do {
			i--;
			value = (value << 8) + s.charAt(i);
		} while (i > 0);
		return value;
	}

	public static String strValue(int value) {
		byte bytes[] = new byte[4];
		for (int i = 0; i < 4; i++) {
			bytes[i] = (byte) (value & 0xFF);
			value >>>= 8;
		}
		return new String(bytes);
	}

	public static String serialize(Object o) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeObject(o);
			out.flush();
			out.close();
			return new String(baos.toByteArray(), IoUtil.CHARSET);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static <T> T deserialize(String s) {
		byte[] bytes = s.getBytes(IoUtil.CHARSET);
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

		try {
			ObjectInputStream in = new ObjectInputStream(bais);
			@SuppressWarnings("unchecked")
			T t = (T) in.readObject();
			return t;
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
