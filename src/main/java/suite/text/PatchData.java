package suite.text;

import java.util.Iterator;
import java.util.List;

public class PatchData implements Iterable<PatchDataSegment> {

	private List<PatchDataSegment> patchDataSegments;

	public PatchData(List<PatchDataSegment> patchDataSegments) {
		this.patchDataSegments = patchDataSegments;
	}

	@Override
	public Iterator<PatchDataSegment> iterator() {
		return patchDataSegments.iterator();
	}

	public List<PatchDataSegment> getPatchDataSegments() {
		return patchDataSegments;
	}

}
