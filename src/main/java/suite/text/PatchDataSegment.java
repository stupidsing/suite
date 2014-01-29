package suite.text;

import suite.net.Bytes;

public class PatchDataSegment {

	// dataSegmentAye.bytes == dataSegmentBee.bytes for common segment
	private DataSegment dataSegmentAye;
	private DataSegment dataSegmentBee;

	public PatchDataSegment(int startAye, int startBee, Bytes bytesAye, Bytes bytesBee) {
		this(new DataSegment(startAye, startAye + bytesAye.size(), bytesAye) //
				, new DataSegment(startBee, startBee + bytesBee.size(), bytesBee));
	}

	public PatchDataSegment(DataSegment dataSegmentAye, DataSegment dataSegmentBee) {
		this.dataSegmentAye = dataSegmentAye;
		this.dataSegmentBee = dataSegmentBee;
	}

	public boolean isChanged() {
		return dataSegmentAye.getBytes() != dataSegmentBee.getBytes();
	}

	public PatchDataSegment adjust(int offsetAye, int offsetBee) {
		DataSegment dataSegmentAye1 = dataSegmentAye.adjust(offsetAye);
		DataSegment dataSegmentBee1 = dataSegmentBee.adjust(offsetBee);
		return new PatchDataSegment(dataSegmentAye1, dataSegmentBee1);
	}

	public PatchDataSegment reverse() {
		return new PatchDataSegment(dataSegmentBee, dataSegmentAye);
	}

	public DataSegment getDataSegmentAye() {
		return dataSegmentAye;
	}

	public DataSegment getDataSegmentBee() {
		return dataSegmentBee;
	}

}
