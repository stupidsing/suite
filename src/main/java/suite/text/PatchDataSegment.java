package suite.text;

import java.util.Objects;

import suite.primitive.Bytes;
import suite.util.To;

public class PatchDataSegment {

	// Original data and new data
	// dataSegmentOld.bytes == dataSegmentNew.bytes for common segment
	public final DataSegment dataSegmentOld;
	public final DataSegment dataSegmentNew;

	public static PatchDataSegment of(int startOld, int startNew, Bytes bytes) {
		return of(startOld, startNew, bytes, bytes);
	}

	public static PatchDataSegment of(int startOld, int startNew, Bytes bytesOld, Bytes bytesNew) {
		return of( //
				DataSegment.of(startOld, startOld + bytesOld.size(), bytesOld), //
				DataSegment.of(startNew, startNew + bytesNew.size(), bytesNew));
	}

	public static PatchDataSegment of(DataSegment dataSegmentOld, DataSegment dataSegmentNew) {
		return new PatchDataSegment(dataSegmentOld, dataSegmentNew);
	}

	private PatchDataSegment(DataSegment dataSegmentOld, DataSegment dataSegmentNew) {
		this.dataSegmentOld = dataSegmentOld;
		this.dataSegmentNew = dataSegmentNew;
	}

	public boolean isEmpty() {
		return dataSegmentOld.isEmpty() && dataSegmentNew.isEmpty();
	}

	public boolean isChanged() {
		return !Objects.equals(dataSegmentOld.bytes, dataSegmentNew.bytes);
	}

	public PatchDataSegment adjust(int offsetOld, int offsetNew) {
		DataSegment dataSegmentOld1 = dataSegmentOld.adjust(offsetOld);
		DataSegment dataSegmentNew1 = dataSegmentNew.adjust(offsetNew);
		return new PatchDataSegment(dataSegmentOld1, dataSegmentNew1);
	}

	public PatchDataSegment reverse() {
		return new PatchDataSegment(dataSegmentNew, dataSegmentOld);
	}

	@Override
	public String toString() {
		boolean isChanged = isChanged();
		String s0 = dataSegmentOld + (isChanged ? "!" : "=") + dataSegmentNew;
		String s;

		if (isChanged)
			s = s0 + "[" + To.string(dataSegmentOld.bytes) + "|" + To.string(dataSegmentNew.bytes) + "],";
		else
			s = s0 + "[" + To.string(dataSegmentOld.bytes) + "],";

		return s;
	}

}
