package suite.text;

import java.util.Iterator;
import java.util.List;

import suite.util.To;

public class PatchData implements Iterable<PatchDataSegment> {

	private List<PatchDataSegment> patchDataSegments;

	public PatchData(List<PatchDataSegment> patchDataSegments) {
		this.patchDataSegments = patchDataSegments;
	}

	public void write(StringBuilder sb) {
		for (PatchDataSegment pds : patchDataSegments)
			if (!pds.isEmpty()) {
				boolean isChanged = pds.isChanged();
				DataSegment dsa = pds.getDataSegmentAye();
				DataSegment dsb = pds.getDataSegmentBee();

				sb.append(dsa.getStart() + ":" + dsa.getEnd() //
						+ "|" + dsb.getStart() + ":" + dsb.getEnd() //
						+ "|" + (isChanged ? "Y" : "N"));

				if (isChanged)
					sb.append("<<" + To.string(dsa.getBytes()) + "|" + To.string(dsb.getBytes()) + ">>");
				else
					sb.append("<<" + To.string(dsa.getBytes()) + ">>");
			}
	}

	@Override
	public Iterator<PatchDataSegment> iterator() {
		return patchDataSegments.iterator();
	}

	public List<PatchDataSegment> getPatchDataSegments() {
		return patchDataSegments;
	}

}
