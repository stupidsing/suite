package suite.text;

import java.util.Iterator;
import java.util.List;

public class PatchData implements Iterable<PatchDataSegment> {

	public final List<PatchDataSegment> patchDataSegments;

	public static PatchData of(List<PatchDataSegment> patchDataSegments) {
		return new PatchData(patchDataSegments);
	}

	private PatchData(List<PatchDataSegment> patchDataSegments) {
		this.patchDataSegments = patchDataSegments;
	}

	public boolean isChanged() {
		boolean isChanged = false;
		for (PatchDataSegment patchDataSegment : patchDataSegments)
			isChanged |= patchDataSegment.isChanged();
		return isChanged;
	}

	@Override
	public Iterator<PatchDataSegment> iterator() {
		return patchDataSegments.iterator();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (PatchDataSegment pds : patchDataSegments)
			if (!pds.isEmpty())
				sb.append(pds.toString());

		return sb.toString();
	}

}
