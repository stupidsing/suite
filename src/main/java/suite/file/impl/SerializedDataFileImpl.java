package suite.file.impl;

import java.io.DataInputStream;
import java.io.IOException;

import suite.file.DataFile;
import suite.file.SerializedFile;
import suite.primitive.Bytes;
import suite.util.Rethrow;
import suite.util.Serialize.Serializer;

/**
 * Persists B-tree pages to file on disk.
 *
 * The following must holds when using this class:
 *
 * sizeof(char) + 2 * sizeof(int) + branchFactor * branchPointerSize <= pageSize
 *
 * where branchPointerSize = max(sizeof(int), sizeof(Value))
 */
public class SerializedDataFileImpl<Pointer, V> implements SerializedFile<Pointer, V> {

	private DataFile<Pointer> dataFile;
	private Serializer<V> serializer;

	public SerializedDataFileImpl(DataFile<Pointer> dataFile, Serializer<V> serializer) {
		this.dataFile = dataFile;
		this.serializer = serializer;
	}

	@Override
	public void close() throws IOException {
		dataFile.close();
	}

	@Override
	public void sync() {
		dataFile.sync();
	}

	@Override
	public V load(Pointer pointer) {
		return Rethrow.ioException(() -> serializer.read(new DataInputStream(dataFile.load(pointer).asInputStream())));
	}

	@Override
	public void save(Pointer pointer, V value) {
		dataFile.save(pointer, Rethrow.ioException(() -> Bytes.of(dataOutput -> serializer.write(dataOutput, value))));
	}

}
