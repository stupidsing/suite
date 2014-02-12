package suite.text;

import suite.primitive.Bytes;

public class DataSegment extends Segment {

	private Bytes bytes;

	public DataSegment(int start, int end, Bytes bytes) {
		super(start, end);
		this.bytes = bytes;
	}

	public DataSegment left(int pos) {
		return new DataSegment(getStart(), pos, bytes.subbytes(0, pos - getStart()));
	}

	public DataSegment right(int pos) {
		return new DataSegment(pos, getEnd(), bytes.subbytes(pos - getStart()));
	}

	public DataSegment adjust(int offset) {
		return new DataSegment(getStart() + offset, getEnd() + offset, bytes);
	}

	public Bytes getBytes() {
		return bytes;
	}

}
