package suite.text;

import java.util.Objects;

import suite.primitive.Bytes;
import suite.util.To;

public class PatchDataSegment {

	// Original data and new data
	// dataSegmentOrg.bytes == dataSegmentNew.bytes for common segment
	private DataSegment dataSegmentOrg;
	private DataSegment dataSegmentNew;

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
		return !Objects.equals(dataSegmentOrg.getBytes(), dataSegmentNew.getBytes());
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
		DataSegment dsOrg = getDataSegmentOrg();
		DataSegment dsNew = getDataSegmentNew();
		String s0 = dsOrg + (isChanged ? "!" : "=") + dsNew;
		String s;

		if (isChanged)
			s = s0 + "[" + To.string(dsOrg.getBytes()) + "|" + To.string(dsNew.getBytes()) + "],";
		else
			s = s0 + "[" + To.string(dsOrg.getBytes()) + "],";

		return s;
	}

	public DataSegment getDataSegmentOrg() {
		return dataSegmentOrg;
	}

	public DataSegment getDataSegmentNew() {
		return dataSegmentNew;
	}

}
