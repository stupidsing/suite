package suite.file.impl;

import suite.file.ExtentAllocator.Extent;
import suite.file.ExtentFile;
import suite.file.SerializedExtentFile;
import suite.util.SerializeUtil.Serializer;

public class SerializedExtentFileImpl<V> extends SerializedDataFileImpl<Extent, V>implements SerializedExtentFile<V> {

	public SerializedExtentFileImpl(ExtentFile extentFile, Serializer<V> serializer) {
		super(extentFile, serializer);
	}

}
