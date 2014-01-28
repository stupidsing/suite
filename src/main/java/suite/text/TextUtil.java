package suite.text;

import java.util.ArrayList;
import java.util.List;

import suite.algo.LongestCommonContinuousSequence;
import suite.net.Bytes;
import suite.net.Bytes.BytesBuilder;
import suite.util.Pair;
import suite.util.Util;

public class TextUtil {

	public class ConflictException extends Exception {
		private static final long serialVersionUID = 1l;
	}

	private class MergeData {
		private List<PatchSegment> patchSegments;
		private int pos;
		private int offset;

		public MergeData(List<PatchSegment> patchSegments) {
			this.patchSegments = patchSegments;
		}
	}

	public Patch diff(Bytes bytesAye, Bytes bytesBee) {
		LongestCommonContinuousSequence lccs = new LongestCommonContinuousSequence();
		Pair<Segment, Segment> diff = Util.first(lccs.lccs(bytesAye, bytesBee));

		Segment segmentAye = diff.t0, segmentBee = diff.t1;
		int a0 = 0, a1 = segmentAye.getStart(), a2 = segmentAye.getEnd();
		int b0 = 0, b1 = segmentBee.getStart(), b2 = segmentBee.getEnd();

		PatchSegment patchSegment0 = new PatchSegment(a0, b0 //
				, bytesAye.subbytes(a0, a1) //
				, bytesBee.subbytes(b0, b1));

		PatchSegment patchSegment1 = new PatchSegment(segmentAye, segmentBee);

		PatchSegment patchSegment2 = new PatchSegment(a2, b2 //
				, bytesAye.subbytes(a2) //
				, bytesBee.subbytes(b2));

		List<PatchSegment> patchSegments = new ArrayList<>();
		patchSegments.addAll(diff(patchSegment0).getPatchSegments());
		patchSegments.add(patchSegment1);
		patchSegments.addAll(diff(patchSegment2).getPatchSegments());

		return new Patch(patchSegments);
	}

	private Patch diff(PatchSegment patchSegment0) {
		Segment segmentAye = patchSegment0.getSegmentAye();
		Segment segmentBee = patchSegment0.getSegmentBee();
		List<PatchSegment> patchSegments = diff(segmentAye.getBytes(), segmentBee.getBytes()).getPatchSegments();
		List<PatchSegment> patchSegments1 = new ArrayList<>();

		for (PatchSegment patchSegment : patchSegments)
			patchSegments1.add(patchSegment.adjust(segmentAye.getStart(), segmentBee.getStart()));

		return new Patch(patchSegments1);
	}

	public Bytes patch(Bytes bytes, Patch patch) {
		BytesBuilder bytesBuilder = new BytesBuilder();

		for (PatchSegment patchSegment : patch)
			if (!patchSegment.isChanged()) {
				Segment segmentAye = patchSegment.getSegmentAye();
				bytesBuilder.append(bytes.subbytes(segmentAye.getStart(), segmentAye.getEnd()));
			} else
				bytesBuilder.append(patchSegment.getSegmentBee().getBytes());

		return bytesBuilder.toBytes();
	}

	public Patch merge(Patch patchX, Patch patchY) throws ConflictException {
		List<PatchSegment> merged = new ArrayList<>();
		MergeData mdx = new MergeData(patchX.getPatchSegments());
		MergeData mdy = new MergeData(patchY.getPatchSegments());
		boolean isAvailX, isAvailY;
		int start = 0;

		while ((isAvailX = mdx.pos < mdx.patchSegments.size()) && (isAvailY = mdy.pos < mdy.patchSegments.size())) {
			boolean isAdvanceX;

			if (isAvailX && isAvailY) {
				int endX = mdx.patchSegments.get(mdx.pos).getSegmentAye().getEnd();
				int endY = mdy.patchSegments.get(mdy.pos).getSegmentAye().getEnd();
				isAdvanceX = endX < endY;
			} else
				isAdvanceX = isAvailX;

			start = isAdvanceX ? advance(mdx, mdy, start, merged) : advance(mdy, mdx, start, merged);
		}

		return new Patch(merged);
	}

	private int advance(MergeData sdx, MergeData sdy, int start, List<PatchSegment> patchSegments1) throws ConflictException {
		PatchSegment patchSegmentX = sdx.patchSegments.get(sdx.pos);
		PatchSegment patchSegmentY = sdy.patchSegments.get(sdy.pos);

		if (!patchSegmentY.isChanged()) {
			Segment segmentAye1 = patchSegmentX.getSegmentAye().right(start);
			Segment segmentBee1 = patchSegmentX.getSegmentBee().right(start + sdx.offset);
			patchSegments1.add(new PatchSegment(segmentAye1, segmentBee1.adjust(sdy.offset)));

			sdx.offset += patchSegmentX.getSegmentBee().length() - patchSegmentX.getSegmentAye().length();
			start = patchSegmentX.getSegmentAye().getEnd();
			sdx.pos++;
		} else
			throw new ConflictException();

		return start;
	}

}
