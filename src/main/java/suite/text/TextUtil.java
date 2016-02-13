package suite.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import suite.adt.Pair;
import suite.lcs.Lccs;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;

public class TextUtil {

	public class ConflictException extends Exception {
		private static final long serialVersionUID = 1l;
	}

	private class MergeData {
		private List<PatchDataSegment> patchDataSegments;
		private int pos;
		private int offset;

		public MergeData(List<PatchDataSegment> patchDataSegments) {
			this.patchDataSegments = new ArrayList<>(patchDataSegments);
		}

		public void next() {
			PatchDataSegment pds = patchDataSegments.get(pos);
			offset += pds.dataSegmentNew.length() - pds.dataSegmentOld.length();
			pos++;
		}
	}

	public PatchData diff(Bytes bytesOld, Bytes bytesNew) {
		Lccs lccs = new Lccs();
		Pair<Segment, Segment> diff = lccs.lccs(bytesOld, bytesNew);
		Segment so = diff.t0, sn = diff.t1;
		int o0 = 0, o1 = so.start, o2 = so.end;
		int n0 = 0, n1 = sn.start, n2 = sn.end;

		if (!so.isEmpty() && !sn.isEmpty()) {
			PatchDataSegment pds0 = PatchDataSegment.of(o0, n0 //
					, bytesOld.subbytes(o0, o1) //
					, bytesNew.subbytes(n0, n1));

			PatchDataSegment pds1 = PatchDataSegment.of(o1, n1 //
					, bytesOld.subbytes(o1, o2) //
			);

			PatchDataSegment pds2 = PatchDataSegment.of(o2, n2 //
					, bytesOld.subbytes(o2) //
					, bytesNew.subbytes(n2));

			List<PatchDataSegment> pdsList = new ArrayList<>();
			pdsList.addAll(diff(pds0).patchDataSegments);
			pdsList.add(pds1);
			pdsList.addAll(diff(pds2).patchDataSegments);

			return PatchData.of(pdsList);
		} else
			return PatchData.of(Arrays.asList( //
					PatchDataSegment.of(0, 0, bytesOld, bytesNew)));
	}

	private PatchData diff(PatchDataSegment patchDataSegment) {
		DataSegment dsOld = patchDataSegment.dataSegmentOld;
		DataSegment dsNew = patchDataSegment.dataSegmentNew;
		PatchData subPatchData = diff(dsOld.bytes, dsNew.bytes);
		List<PatchDataSegment> pdsList = new ArrayList<>();

		for (PatchDataSegment pds : subPatchData.patchDataSegments)
			pdsList.add(pds.adjust(dsOld.start, dsNew.start));

		return PatchData.of(pdsList);
	}

	public Bytes patch(Bytes bytes, PatchData patchData) {
		BytesBuilder bytesBuilder = new BytesBuilder();

		for (PatchDataSegment pds : patchData)
			if (!pds.isChanged()) {
				DataSegment dso = pds.dataSegmentOld;
				bytesBuilder.append(bytes.subbytes(dso.start, dso.end));
			} else
				bytesBuilder.append(pds.dataSegmentNew.bytes);

		return bytesBuilder.toBytes();
	}

	public PatchData merge(PatchData patchDataX, PatchData patchDataY) throws ConflictException {
		List<PatchDataSegment> merged = new ArrayList<>();
		MergeData mdx = new MergeData(patchDataX.patchDataSegments);
		MergeData mdy = new MergeData(patchDataY.patchDataSegments);
		boolean isAvailX, isAvailY;
		int start = 0;

		while ((isAvailX = mdx.pos < mdx.patchDataSegments.size()) && (isAvailY = mdy.pos < mdy.patchDataSegments.size())) {
			boolean isAdvanceX;

			if (isAvailX && isAvailY) {
				PatchDataSegment pdsx = mdx.patchDataSegments.get(mdx.pos);
				PatchDataSegment pdsy = mdy.patchDataSegments.get(mdy.pos);
				int endx = pdsx.dataSegmentOld.end;
				int endy = pdsy.dataSegmentOld.end;

				if (endx == endy)
					isAdvanceX = !pdsy.isChanged();
				else
					isAdvanceX = endx < endy;
			} else
				isAdvanceX = isAvailX;

			start = isAdvanceX ? advance(mdx, mdy, start, merged) : advance(mdy, mdx, start, merged);
		}

		return PatchData.of(merged);
	}

	private int advance(MergeData mdx, MergeData mdy, int start, List<PatchDataSegment> pdsList) throws ConflictException {
		PatchDataSegment pdsx = mdx.patchDataSegments.get(mdx.pos);
		PatchDataSegment pdsy = mdy.patchDataSegments.get(mdy.pos);
		DataSegment dsxOld = pdsx.dataSegmentOld, dsxNew = pdsx.dataSegmentNew;
		DataSegment dsyOld = pdsy.dataSegmentOld, dsyNew = pdsy.dataSegmentNew;

		// If both patches do not overlap in original sections,
		// they can be merged
		boolean isSeparate = dsxOld.end <= dsyOld.start;

		// If the longer patch segment is not changing anything,
		// they can be merged
		boolean isUnchanged = !pdsy.isChanged();

		// If both patches have same starting (head) content in original and
		// target sections, the head parts can be merged
		int dsxOldLength = dsxOld.length(), dsxNewLength = dsxNew.length();
		int dsyOldLength = dsyOld.length(), dsyNewLength = dsyNew.length();
		boolean isMappingsAgree = dsxOld.start == dsyOld.start && dsxOldLength <= dsyOldLength //
				&& dsxNewLength <= dsyNewLength //
				&& Objects.equals(dsxOld.bytes, dsyOld.bytes.subbytes(0, dsxOldLength))
				&& Objects.equals(dsxNew.bytes, dsyNew.bytes.subbytes(0, dsyOldLength));

		if (isSeparate || isUnchanged || isMappingsAgree) {
			DataSegment dsOld1 = dsxOld.right(start);
			DataSegment dsNew1 = dsxNew.right(start + mdx.offset);
			pdsList.add(PatchDataSegment.of(dsOld1, dsNew1.adjust(mdy.offset)));

			// If only the head part can merge, add back tail parts to the lists
			if (isMappingsAgree) {
				DataSegment dsyOld0 = dsyOld.left(dsxOld.end);
				DataSegment dsyNew0 = dsyNew.left(dsyNew.start + dsxNewLength);
				DataSegment dsyOld1 = dsyOld.right(dsxOld.end);
				DataSegment dsyNew1 = dsyNew.right(dsyNew.start + dsxNewLength);
				mdy.patchDataSegments.set(mdy.pos, PatchDataSegment.of(dsyOld0, dsyNew0));
				mdy.next();
				mdy.patchDataSegments.add(mdy.pos, PatchDataSegment.of(dsyOld1, dsyNew1));
			}

			mdx.next();
			return dsxOld.end;
		} else
			throw new ConflictException();
	}

}
