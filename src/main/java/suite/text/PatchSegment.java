package suite.text;

import suite.net.Bytes;

public class PatchSegment {

	// segmentAye.bytes == segmentBee.bytes for common segment
	private Segment segmentAye;
	private Segment segmentBee;

	public PatchSegment(int startAye, int startBee, Bytes bytesAye, Bytes bytesBee) {
		this(new Segment(startAye, startAye + bytesAye.size(), bytesAye) //
				, new Segment(startBee, startBee + bytesBee.size(), bytesBee));
	}

	public PatchSegment(Segment segmentAye, Segment segmentBee) {
		this.segmentAye = segmentAye;
		this.segmentBee = segmentBee;
	}

	public boolean isChanged() {
		return segmentAye.getBytes() != segmentBee.getBytes();
	}

	public PatchSegment adjust(int offsetAye, int offsetBee) {
		Segment segmentAye1 = segmentAye.adjust(offsetAye);
		Segment segmentBee1 = segmentBee.adjust(offsetBee);
		return new PatchSegment(segmentAye1, segmentBee1);
	}

	public PatchSegment reverse() {
		return new PatchSegment(segmentBee, segmentAye);
	}

	public Segment getSegmentAye() {
		return segmentAye;
	}

	public Segment getSegmentBee() {
		return segmentBee;
	}

}
