package suite.text;

import java.util.Objects;

import suite.primitive.Bytes;
import suite.util.To;

public class PatchDataSegment {

	// Original data and new data
	// dataSegmentOrg.bytes == dataSegmentNew.bytes for common segment
	public final DataSegment dataSegmentOrg;
	public final DataSegment dataSegmentNew;

	public static PatchDataSegment of(int startOrg, int startNew, Bytes bytes) {
		return of(startOrg, startNew, bytes, bytes);
	}

	public static PatchDataSegment of(int startOrg, int startNew, Bytes bytesOrg, Bytes bytesNew) {
		return of( //
				DataSegment.of(startOrg, startOrg + bytesOrg.size(), bytesOrg), //
				DataSegment.of(startNew, startNew + bytesNew.size(), bytesNew));
	}

	public static PatchDataSegment of(DataSegment dataSegmentOrg, DataSegment dataSegmentNew) {
		return new PatchDataSegment(dataSegmentOrg, dataSegmentNew);
	}

	private PatchDataSegment(DataSegment dataSegmentOrg, DataSegment dataSegmentNew) {
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
