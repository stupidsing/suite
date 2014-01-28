package suite.text;

import java.util.Iterator;
import java.util.List;

public class Patch implements Iterable<PatchSegment> {

	private List<PatchSegment> patchSegments;

	public Patch(List<PatchSegment> patchSegments) {
		this.patchSegments = patchSegments;
	}

	@Override
	public Iterator<PatchSegment> iterator() {
		return patchSegments.iterator();
	}

	public List<PatchSegment> getPatchSegments() {
		return patchSegments;
	}

}
