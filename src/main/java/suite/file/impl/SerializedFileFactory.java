package suite.file.impl;

import java.io.DataInputStream;
import java.io.IOException;

import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.primitive.Bytes;
import suite.util.Rethrow;
import suite.util.Serialize.Serializer;

public class SerializedFileFactory {

	public static <V> SerializedPageFile<V> serialized(PageFile pageFile, Serializer<V> serializer) {
		return new SerializedPageFile<V>() {
			public void close() throws IOException {
				pageFile.close();
			}

			public void sync() {
				pageFile.sync();
			}

			public V load(int pointer) {
				return Rethrow.ex(() -> serializer.read(new DataInputStream(pageFile.load(pointer).asInputStream())));
			}

			public void save(int pointer, V value) {
				pageFile.save(pointer, Rethrow.ex(() -> Bytes.of(dataOutput -> serializer.write(dataOutput, value))));
			}
		};
	}

}
