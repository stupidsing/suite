package suite.text;

import java.util.Objects;

import suite.primitive.Bytes;
import suite.util.To;

public class PatchDataSegment {

	// Original data and new data
	// dataSegmentOrg.bytes == dataSegmentNew.bytes for common segment
	public final DataSegment dataSegmentOrg;
	public final DataSegment dataSegmentNew;

	public PatchDataSegment(int startOrg, int startNew, Bytes bytes) {
		this(startOrg, startNew, bytes, bytes);
	}

	public PatchDataSegment(int startOrg, int startNew, Bytes bytesOrg, Bytes bytesNew) {
		this(new DataSegment(startOrg, startOrg + bytesOrg.size(), bytesOrg) //
				, new DataSegment(startNew, startNew + bytesNew.size(), bytesNew));
	}

	public PatchDataSegment(DataSegment dataSegmentOrg, DataSegment dataSegmentNew) {
		this.dataSegmentOrg = dataSegmentOrg;
		this.dataSegmentNew = dataSegmentNew;
	}

	public boolean isEmpty() {
		return dataSegmentOrg.isEmpty() && dataSegmentNew.isEmpty();
	}

	public boolean isChanged() {
		return !Objects.equals(dataSegmentOrg.bytes, dataSegmentNew.bytes);
	}

	public PatchDataSegment adjust(int offsetOrg, int offsetNew) {
		DataSegment dataSegmentOrg1 = dataSegmentOrg.adjust(offsetOrg);
		DataSegment dataSegmentNew1 = dataSegmentNew.adjust(offsetNew);
		return new PatchDataSegment(dataSegmentOrg1, dataSegmentNew1);
	}

	public PatchDataSegment reverse() {
		return new PatchDataSegment(dataSegmentNew, dataSegmentOrg);
	}

	@Override
	public String toString() {
		boolean isChanged = isChanged();
		String s0 = dataSegmentOrg + (isChanged ? "!" : "=") + dataSegmentNew;
		String s;

		if (isChanged)
			s = s0 + "[" + To.string(dataSegmentOrg.bytes) + "|" + To.string(dataSegmentNew.bytes) + "],";
		else
			s = s0 + "[" + To.string(dataSegmentOrg.bytes) + "],";

		return s;
	}

}
