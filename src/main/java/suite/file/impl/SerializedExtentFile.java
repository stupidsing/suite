package suite.file.impl;

import suite.file.DataFile;
import suite.file.ExtentAllocator.Extent;
import suite.util.SerializeUtil.Serializer;

public class SerializedExtentFile<V> extends SerializedDataFile<Extent, V> {

	public SerializedExtentFile(DataFile<Extent> dataFile, Serializer<V> serializer) {
		super(dataFile, serializer);
	}

}
