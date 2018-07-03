package suite.file.impl;

import static suite.util.Friends.rethrow;

import java.io.IOException;

import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.serialize.SerInput;
import suite.serialize.Serialize.Serializer;
import suite.streamlet.As;
import suite.util.To;

public class SerializedFileFactory {

	public static <V> SerializedPageFile<V> serialized(PageFile pageFile, Serializer<V> serializer) {
		return new SerializedPageFile<>() {
			public void close() throws IOException {
				pageFile.close();
			}

			public void sync() {
				pageFile.sync();
			}

			public V load(int pointer) {
				return rethrow(() -> serializer.read(SerInput.of(pageFile.load(pointer).collect(As::inputStream))));
			}

			public void save(int pointer, V value) {
				pageFile.save(pointer, To.bytes(so -> serializer.write(so, value)));
			}
		};
	}

}
