package suite.file.impl;

import suite.file.DataFile;
import suite.util.SerializeUtil.Serializer;

public class SerializedPageFileImpl<V> extends SerializedDataFileImpl<Integer, V> {

	public SerializedPageFileImpl(DataFile<Integer> dataFile, Serializer<V> serializer) {
		super(dataFile, serializer);
	}

}
