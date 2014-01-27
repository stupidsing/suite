package suite.text;

import java.util.Arrays;
import java.util.List;

import suite.net.Bytes;

public class Segment {

	private int start, end;
	private Bytes bytes;

	public static final Segment nil = new Segment(0, 0, Bytes.emptyBytes);

	public Segment(int start, int end, Bytes bytes) {
		this.start = start;
		this.end = end;
		this.bytes = bytes;
	}

	public Segment adjust(int offset) {
		return new Segment(start + offset, end + offset, bytes);
	}

	public List<Segment> split(int offset) {
		if (offset < start)
			return Arrays.asList(nil, this);
		else if (offset < end) {
			Segment segment0 = new Segment(start, offset, bytes.subbytes(0, offset - start));
			Segment segment1 = new Segment(offset, end, bytes.subbytes(offset - start));
			return Arrays.asList(segment0, segment1);
		} else
			return Arrays.asList(this, nil);
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
