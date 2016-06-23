package suite.file.impl;

import java.io.DataInputStream;
import java.io.IOException;

import suite.file.DataFile;
import suite.file.PageFile;
import suite.file.SerializedFile;
import suite.file.SerializedPageFile;
import suite.primitive.Bytes;
import suite.util.Rethrow;
import suite.util.Serialize.Serializer;

public class SerializedFileFactory {

	public static <Pointer, V> SerializedFile<Pointer, V> serialized(DataFile<Pointer> dataFile, Serializer<V> serializer) {
		return new SerializedFile<Pointer, V>() {
			public void close() throws IOException {
				dataFile.close();
			}

			public void sync() {
				dataFile.sync();
			}

			public V load(Pointer pointer) {
				return Rethrow.ioException(() -> serializer.read(new DataInputStream(dataFile.load(pointer).asInputStream())));
			}

			public void save(Pointer pointer, V value) {
				dataFile.save(pointer, Rethrow.ioException(() -> Bytes.of(dataOutput -> serializer.write(dataOutput, value))));
			}
		};
	}

	public static <V> SerializedPageFile<V> serialized(PageFile pageFile, Serializer<V> serializer) {
		SerializedPageFile<V> dataFile = serialized(pageFile, serializer);

		return new SerializedPageFile<V>() {
			public void close() throws IOException {
				dataFile.close();
			}

			public void sync() {
				dataFile.sync();
			}

			public V load(int pointer) {
				return dataFile.load(pointer);
			}

			public void save(int pointer, V value) {
				dataFile.save(pointer, value);
			}
		};
	}

}
