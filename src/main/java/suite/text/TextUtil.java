package suite.text;

import java.util.ArrayList;
import java.util.List;

import suite.algo.LongestCommonContinuousSequence;
import suite.net.Bytes;
import suite.net.Bytes.BytesBuilder;
import suite.primitive.IntList;
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
		IntList intList = Util.first(lccs.lccs(bytesAye.getBytes(), bytesBee.getBytes()));

		PatchSegment patchSegment0 = new PatchSegment(intList //
				, bytesAye.subbytes(0, intList.get(0)) //
				, bytesBee.subbytes(0, intList.get(2)));

		PatchSegment patchSegment1 = new PatchSegment(intList //
				, bytesAye.subbytes(intList.get(0), intList.get(1)));

		PatchSegment patchSegment2 = new PatchSegment(intList //
				, bytesAye.subbytes(intList.get(1), bytesAye.size()) //
				, bytesBee.subbytes(intList.get(3), bytesBee.size()));

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
