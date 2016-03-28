package suite.text;

public class Segment {

	public final int start, end;

	public static Segment of(Segment segment) {
		return Segment.of(segment.start, segment.end);
	}

	public static Segment of(int start, int end) {
		return new Segment(start, end);
	}

	private Segment(int start, int end) {
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
