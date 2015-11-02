package suite.file.impl;

import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.util.FunUtil.Source;
import suite.util.Serialize.Serializer;

public class SerializedPageFileImpl<V> extends SerializedDataFileImpl<Integer, V>implements SerializedPageFile<V> {

	public SerializedPageFileImpl(PageFile pageFile, Serializer<V> serializer, Source<V> source) {
		super(pageFile, serializer, source);
	}

	public SerializedPageFileImpl(PageFile pageFile, Serializer<V> serializer) {
		super(pageFile, serializer);
	}

}
