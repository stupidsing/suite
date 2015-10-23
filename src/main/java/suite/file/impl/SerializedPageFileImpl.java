package suite.file.impl;

import suite.file.DataFile;
import suite.file.SerializedPageFile;
import suite.util.SerializeUtil.Serializer;

public class SerializedPageFileImpl<V> extends SerializedDataFileImpl<Integer, V>implements SerializedPageFile<V> {

	public SerializedPageFileImpl(DataFile<Integer> dataFile, Serializer<V> serializer) {
		super(dataFile, serializer);
	}

}
