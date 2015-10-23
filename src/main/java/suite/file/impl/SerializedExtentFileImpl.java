package suite.file.impl;

import suite.file.DataFile;
import suite.file.ExtentAllocator.Extent;
import suite.file.SerializedExtentFile;
import suite.util.SerializeUtil.Serializer;

public class SerializedExtentFileImpl<V> extends SerializedDataFileImpl<Extent, V>implements SerializedExtentFile<V> {

	public SerializedExtentFileImpl(DataFile<Extent> dataFile, Serializer<V> serializer) {
		super(dataFile, serializer);
	}

}
