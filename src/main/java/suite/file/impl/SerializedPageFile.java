package suite.file.impl;

import suite.file.DataFile;
import suite.util.SerializeUtil.Serializer;

public class SerializedPageFile<V> extends SerializedDataFile<Integer, V> {

	public SerializedPageFile(DataFile<Integer> dataFile, Serializer<V> serializer) {
		super(dataFile, serializer);
	}

}
