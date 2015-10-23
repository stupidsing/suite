package suite.file.impl;

import suite.file.DataFile;
import suite.file.ExtentAllocator.Extent;
import suite.util.SerializeUtil.Serializer;

public class SerializedExtentFileImpl<V> extends SerializedDataFileImpl<Extent, V> {

	public SerializedExtentFileImpl(DataFile<Extent> dataFile, Serializer<V> serializer) {
		super(dataFile, serializer);
	}

}
