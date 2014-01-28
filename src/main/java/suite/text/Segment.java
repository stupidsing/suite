package suite.text;

import suite.net.Bytes;

public class Segment {

	private int start, end;
	private Bytes bytes;

	public Segment(int start, int end, Bytes bytes) {
		this.start = start;
		this.end = end;
		this.bytes = bytes;
	}

	public Segment right(int pos) {
		return new Segment(pos, end, bytes.subbytes(pos - start));
	}

	public Segment adjust(int offset) {
		return new Segment(start + offset, end + offset, bytes);
	}

	public int length() {
		return end - start;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public Bytes getBytes() {
		return bytes;
	}

}
