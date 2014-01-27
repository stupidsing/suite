package suite.text;

import suite.fastlist.IntList;
import suite.net.Bytes;

public class DiffSegment {

	// segmentAye.bytes == segmentBee.bytes for common segment
	private Segment segmentAye;
	private Segment segmentBee;

	public DiffSegment(IntList lccs, Bytes bytes) {
		this(lccs, bytes, bytes);
	}

	public DiffSegment(IntList lccs, Bytes bytes0, Bytes bytes1) {
		this(new Segment(lccs.get(0), lccs.get(1), bytes0), new Segment(lccs.get(2), lccs.get(3), bytes1));
	}

	public DiffSegment(Segment segmentAye, Segment segmentBee) {
		this.segmentAye = segmentAye;
		this.segmentBee = segmentBee;
	}

	public boolean isChanged() {
		return segmentAye.getBytes() != segmentBee.getBytes();
	}

	public int offset() {
		return segmentBee.length() - segmentAye.length();
	}

	public DiffSegment adjust(int offsetAye, int offsetBee) {
		Segment segmentAye1 = segmentAye.adjust(offsetAye);
		Segment segmentBee1 = segmentBee.adjust(offsetBee);
		return new DiffSegment(segmentAye1, segmentBee1);
	}

	public DiffSegment reverse() {
		return new DiffSegment(segmentBee, segmentAye);
	}

	public Segment getSegmentAye() {
		return segmentAye;
	}

	public Segment getSegmentBee() {
		return segmentBee;
	}

}
