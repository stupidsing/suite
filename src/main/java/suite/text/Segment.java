package suite.text;

public class Segment {

	public final int start, end;

	public Segment(Segment segment) {
		this(segment.start, segment.end);
	}

	public Segment(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public boolean isEmpty() {
		return start == end;
	}

	public int length() {
		return end - start;
	}

	@Override
	public String toString() {
		return start + "-" + end;
	}

}
