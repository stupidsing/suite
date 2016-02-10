package suite.text;

import suite.primitive.Bytes;

public class DataSegment extends Segment {

	public final Bytes bytes;

	public static DataSegment of(int start, int end, Bytes bytes) {
		return new DataSegment(start, end, bytes);
	}

	private DataSegment(int start, int end, Bytes bytes) {
		super(start, end);
		this.bytes = bytes;
	}

	public DataSegment left(int pos) {
		return new DataSegment(start, pos, bytes.subbytes(0, pos - start));
	}

	public DataSegment right(int pos) {
		return new DataSegment(pos, end, bytes.subbytes(pos - start));
	}

	public DataSegment adjust(int offset) {
		return new DataSegment(start + offset, end + offset, bytes);
	}

}
