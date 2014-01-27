package suite.text;

import java.util.ArrayList;
import java.util.List;

public class Merge {

	public class ConflictException extends Exception {
		private static final long serialVersionUID = 1l;
	}

	private class SegmentData {
		private List<DiffSegment> diffSegments;
		private int position;
		private int offset;

		public SegmentData(List<DiffSegment> diffSegments) {
			this.diffSegments = diffSegments;
		}
	}

	public List<DiffSegment> merge(List<DiffSegment> diffSegmentsX, List<DiffSegment> diffSegmentsY) throws ConflictException {
		List<DiffSegment> diffSegments1 = new ArrayList<>();
		SegmentData sdx = new SegmentData(diffSegmentsX);
		SegmentData sdy = new SegmentData(diffSegmentsY);
		boolean isAvailableX, isAvailableY;
		int start = 0;

		while ((isAvailableX = sdx.position < sdx.diffSegments.size()) && (isAvailableY = sdy.position < sdy.diffSegments.size())) {
			boolean isAdvanceX;

			if (isAvailableX && isAvailableY) {
				int endX = sdx.diffSegments.get(sdx.position).getSegmentAye().getEnd();
				int endY = sdy.diffSegments.get(sdy.position).getSegmentAye().getEnd();
				isAdvanceX = endX < endY;
			} else
				isAdvanceX = isAvailableX;

			start = isAdvanceX ? advance(sdx, sdy, start, diffSegments1) : advance(sdy, sdx, start, diffSegments1);
		}

		return diffSegments1;
	}

	private int advance(SegmentData sdx, SegmentData sdy, int start, List<DiffSegment> diffSegments1) throws ConflictException {
		DiffSegment diffSegmentX = sdx.diffSegments.get(sdx.position);
		DiffSegment diffSegmentY = sdy.diffSegments.get(sdy.position);

		if (!diffSegmentY.isChanged()) {
			Segment segmentAye1 = diffSegmentX.getSegmentAye().right(start);
			Segment segmentBee1 = diffSegmentX.getSegmentBee().right(start + sdx.offset);
			diffSegments1.add(new DiffSegment(segmentAye1, segmentBee1.adjust(sdy.offset)));

			sdx.offset += diffSegmentX.getSegmentBee().length() - diffSegmentX.getSegmentAye().length();
			start = diffSegmentX.getSegmentAye().getEnd();
			sdx.position++;
		} else
			throw new ConflictException();

		return start;
	}

}
