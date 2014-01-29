package suite.text;

public class Segment {

	private int start, end;

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

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

}
