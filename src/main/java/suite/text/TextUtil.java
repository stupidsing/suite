package suite.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.algo.LongestCommonContinuousSubsequence;
import suite.net.Bytes;
import suite.net.Bytes.BytesBuilder;
import suite.util.Pair;
import suite.util.Util;

public class TextUtil {

	public class ConflictException extends Exception {
		private static final long serialVersionUID = 1l;
	}

	private class MergeData {
		private List<PatchDataSegment> patchDataSegments;
		private int pos;
		private int offset;

		public MergeData(List<PatchDataSegment> patchDataSegments) {
			this.patchDataSegments = patchDataSegments;
		}
	}

	public PatchData diff(Bytes bytesAye, Bytes bytesBee) {
		LongestCommonContinuousSubsequence lccs = new LongestCommonContinuousSubsequence();
		Pair<Segment, Segment> diff = lccs.lccs(bytesAye, bytesBee);
		Segment sa = diff.t0, sb = diff.t1;
		int a0 = 0, a1 = sa.getStart(), a2 = sa.getEnd();
		int b0 = 0, b1 = sb.getStart(), b2 = sb.getEnd();

		if (!sa.isEmpty() && !sb.isEmpty()) {
			PatchDataSegment pds0 = new PatchDataSegment(a0, b0 //
					, bytesAye.subbytes(a0, a1) //
					, bytesBee.subbytes(b0, b1));

			PatchDataSegment pds1 = new PatchDataSegment(a1, b1 //
					, bytesAye.subbytes(a1, a2) //
			);

			PatchDataSegment pds2 = new PatchDataSegment(a2, b2 //
					, bytesAye.subbytes(a2) //
					, bytesBee.subbytes(b2));

			List<PatchDataSegment> pdsList = new ArrayList<>();
			pdsList.addAll(diff(pds0).getPatchDataSegments());
			pdsList.add(pds1);
			pdsList.addAll(diff(pds2).getPatchDataSegments());

			return new PatchData(pdsList);
		} else
			return new PatchData(Arrays.asList( //
					new PatchDataSegment(0, 0, bytesAye, bytesBee)));
	}

	private PatchData diff(PatchDataSegment patchDataSegment) {
		DataSegment dsa = patchDataSegment.getDataSegmentAye();
		DataSegment dsb = patchDataSegment.getDataSegmentBee();
		PatchData subPatchData = diff(dsa.getBytes(), dsb.getBytes());
		List<PatchDataSegment> pdsList = new ArrayList<>();

		for (PatchDataSegment pds : subPatchData.getPatchDataSegments())
			pdsList.add(pds.adjust(dsa.getStart(), dsb.getStart()));

		return new PatchData(pdsList);
	}

	public Bytes patch(Bytes bytes, PatchData patchData) {
		BytesBuilder bytesBuilder = new BytesBuilder();

		for (PatchDataSegment pds : patchData)
			if (!pds.isChanged()) {
				DataSegment dsa = pds.getDataSegmentAye();
				bytesBuilder.append(bytes.subbytes(dsa.getStart(), dsa.getEnd()));
			} else
				bytesBuilder.append(pds.getDataSegmentBee().getBytes());

		return bytesBuilder.toBytes();
	}

	public PatchData merge(PatchData patchDataX, PatchData patchDataY) throws ConflictException {
		List<PatchDataSegment> merged = new ArrayList<>();
		MergeData mdx = new MergeData(patchDataX.getPatchDataSegments());
		MergeData mdy = new MergeData(patchDataY.getPatchDataSegments());
		boolean isAvailX, isAvailY;
		int start = 0;

		while ((isAvailX = mdx.pos < mdx.patchDataSegments.size()) && (isAvailY = mdy.pos < mdy.patchDataSegments.size())) {
			boolean isAdvanceX;

			if (isAvailX && isAvailY) {
				PatchDataSegment pdsx = mdx.patchDataSegments.get(mdx.pos);
				PatchDataSegment pdsy = mdy.patchDataSegments.get(mdy.pos);
				int endX = pdsx.getDataSegmentAye().getEnd();
				int endY = pdsy.getDataSegmentAye().getEnd();

				if (endX == endY)
					isAdvanceX = !pdsy.isChanged();
				else
					isAdvanceX = endX < endY;
			} else
				isAdvanceX = isAvailX;

			start = isAdvanceX ? advance(mdx, mdy, start, merged) : advance(mdy, mdx, start, merged);
		}

		return new PatchData(merged);
	}

	private int advance(MergeData mdx, MergeData mdy, int start, List<PatchDataSegment> pdsList) throws ConflictException {
		PatchDataSegment pdsx = mdx.patchDataSegments.get(mdx.pos);
		PatchDataSegment pdsy = mdy.patchDataSegments.get(mdy.pos);
		DataSegment dsxa = pdsx.getDataSegmentAye(), dsxb = pdsx.getDataSegmentBee();
		DataSegment dsya = pdsy.getDataSegmentAye(), dsyb = pdsy.getDataSegmentBee();
		boolean isSeparate = dsxa.getEnd() <= dsya.getStart();
		boolean isTargetsAgree = dsxa.getStart() == dsya.getStart()
				&& Util.equals(dsxb.getBytes(), dsyb.getBytes().subbytes(0, dsxb.length()));

		if (isSeparate || !pdsy.isChanged() || isTargetsAgree) {
			DataSegment dsa1 = dsxa.right(start);
			DataSegment dsb1 = dsxb.right(start + mdx.offset);

			if (isTargetsAgree) {
				DataSegment dsya1 = dsya.right(dsxa.getEnd());
				DataSegment dsyb1 = dsyb.right(dsyb.getStart() + dsxa.length());
				mdy.patchDataSegments.add(mdy.pos + 1, new PatchDataSegment(dsya1, dsyb1));
			}

			pdsList.add(new PatchDataSegment(dsa1, dsb1.adjust(mdy.offset)));

			mdx.offset += dsxb.length() - dsxa.length();
			mdx.pos++;
			return dsxa.getEnd();
		} else
			throw new ConflictException();
	}

}
