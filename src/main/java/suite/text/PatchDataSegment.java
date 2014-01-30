package suite.text;

import suite.net.Bytes;
import suite.util.To;
import suite.util.Util;

public class PatchDataSegment {

	// dataSegmentAye.bytes == dataSegmentBee.bytes for common segment
	private DataSegment dataSegmentAye;
	private DataSegment dataSegmentBee;

	public PatchDataSegment(int startAye, int startBee, Bytes bytes) {
		this(startAye, startBee, bytes, bytes);
	}

	public PatchDataSegment(int startAye, int startBee, Bytes bytesAye, Bytes bytesBee) {
		this(new DataSegment(startAye, startAye + bytesAye.size(), bytesAye) //
				, new DataSegment(startBee, startBee + bytesBee.size(), bytesBee));
	}

	public PatchDataSegment(DataSegment dataSegmentAye, DataSegment dataSegmentBee) {
		this.dataSegmentAye = dataSegmentAye;
		this.dataSegmentBee = dataSegmentBee;
	}

	public boolean isEmpty() {
		return dataSegmentAye.isEmpty() && dataSegmentBee.isEmpty();
	}

	public boolean isChanged() {
		return !Util.equals(dataSegmentAye.getBytes(), dataSegmentBee.getBytes());
	}

	public PatchDataSegment adjust(int offsetAye, int offsetBee) {
		DataSegment dataSegmentAye1 = dataSegmentAye.adjust(offsetAye);
		DataSegment dataSegmentBee1 = dataSegmentBee.adjust(offsetBee);
		return new PatchDataSegment(dataSegmentAye1, dataSegmentBee1);
	}

	public PatchDataSegment reverse() {
		return new PatchDataSegment(dataSegmentBee, dataSegmentAye);
	}

	@Override
	public String toString() {
		boolean isChanged = isChanged();
		DataSegment dsa = getDataSegmentAye();
		DataSegment dsb = getDataSegmentBee();
		String s0 = dsa + (isChanged ? "!" : "=") + dsb;
		String s;

		if (isChanged)
			s = s0 + "[" + To.string(dsa.getBytes()) + "|" + To.string(dsb.getBytes()) + "],";
		else
			s = s0 + "[" + To.string(dsa.getBytes()) + "],";

		return s;
	}

	public DataSegment getDataSegmentAye() {
		return dataSegmentAye;
	}

	public DataSegment getDataSegmentBee() {
		return dataSegmentBee;
	}

}
