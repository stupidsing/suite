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
			offset += pds.dataSegmentNew.length() - pds.dataSegmentOrg.length();
			pos++;
		}
	}

	public PatchData diff(Bytes bytesOrg, Bytes bytesNew) {
		Lccs lccs = new Lccs();
		Pair<Segment, Segment> diff = lccs.lccs(bytesOrg, bytesNew);
		Segment so = diff.t0, sn = diff.t1;
		int o0 = 0, o1 = so.start, o2 = so.end;
		int n0 = 0, n1 = sn.start, n2 = sn.end;

		if (!so.isEmpty() && !sn.isEmpty()) {
			PatchDataSegment pds0 = PatchDataSegment.of(o0, n0 //
					, bytesOrg.subbytes(o0, o1) //
					, bytesNew.subbytes(n0, n1));

			PatchDataSegment pds1 = PatchDataSegment.of(o1, n1 //
					, bytesOrg.subbytes(o1, o2) //
			);

			PatchDataSegment pds2 = PatchDataSegment.of(o2, n2 //
					, bytesOrg.subbytes(o2) //
					, bytesNew.subbytes(n2));

			List<PatchDataSegment> pdsList = new ArrayList<>();
			pdsList.addAll(diff(pds0).patchDataSegments);
			pdsList.add(pds1);
			pdsList.addAll(diff(pds2).patchDataSegments);

			return PatchData.of(pdsList);
		} else
			return PatchData.of(Arrays.asList( //
					PatchDataSegment.of(0, 0, bytesOrg, bytesNew)));
	}

	private PatchData diff(PatchDataSegment patchDataSegment) {
		DataSegment dsOrg = patchDataSegment.dataSegmentOrg;
		DataSegment dsNew = patchDataSegment.dataSegmentNew;
		PatchData subPatchData = diff(dsOrg.bytes, dsNew.bytes);
		List<PatchDataSegment> pdsList = new ArrayList<>();

		for (PatchDataSegment pds : subPatchData.patchDataSegments)
			pdsList.add(pds.adjust(dsOrg.start, dsNew.start));

		return PatchData.of(pdsList);
	}

	public Bytes patch(Bytes bytes, PatchData patchData) {
		BytesBuilder bytesBuilder = new BytesBuilder();

		for (PatchDataSegment pds : patchData)
			if (!pds.isChanged()) {
				DataSegment dso = pds.dataSegmentOrg;
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
				int endx = pdsx.dataSegmentOrg.end;
				int endy = pdsy.dataSegmentOrg.end;

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
		DataSegment dsxOrg = pdsx.dataSegmentOrg, dsxNew = pdsx.dataSegmentNew;
		DataSegment dsyOrg = pdsy.dataSegmentOrg, dsyNew = pdsy.dataSegmentNew;

		// If both patches do not overlap in original sections,
		// they can be merged
		boolean isSeparate = dsxOrg.end <= dsyOrg.start;

		// If the longer patch segment is not changing anything,
		// they can be merged
		boolean isUnchanged = !pdsy.isChanged();

		// If both patches have same starting (head) content in original and
		// target sections, the head parts can be merged
		int dsxOrgLength = dsxOrg.length(), dsxNewLength = dsxNew.length();
		int dsyOrgLength = dsyOrg.length(), dsyNewLength = dsyNew.length();
		boolean isMappingsAgree = dsxOrg.start == dsyOrg.start && dsxOrgLength <= dsyOrgLength //
				&& dsxNewLength <= dsyNewLength //
				&& Objects.equals(dsxOrg.bytes, dsyOrg.bytes.subbytes(0, dsxOrgLength))
				&& Objects.equals(dsxNew.bytes, dsyNew.bytes.subbytes(0, dsyOrgLength));

		if (isSeparate || isUnchanged || isMappingsAgree) {
			DataSegment dsOrg1 = dsxOrg.right(start);
			DataSegment dsNew1 = dsxNew.right(start + mdx.offset);
			pdsList.add(PatchDataSegment.of(dsOrg1, dsNew1.adjust(mdy.offset)));

			// If only the head part can merge, add back tail parts to the lists
			if (isMappingsAgree) {
				DataSegment dsyOrg0 = dsyOrg.left(dsxOrg.end);
				DataSegment dsyNew0 = dsyNew.left(dsyNew.start + dsxNewLength);
				DataSegment dsyOrg1 = dsyOrg.right(dsxOrg.end);
				DataSegment dsyNew1 = dsyNew.right(dsyNew.start + dsxNewLength);
				mdy.patchDataSegments.set(mdy.pos, PatchDataSegment.of(dsyOrg0, dsyNew0));
				mdy.next();
				mdy.patchDataSegments.add(mdy.pos, PatchDataSegment.of(dsyOrg1, dsyNew1));
			}

			mdx.next();
			return dsxOrg.end;
		} else
			throw new ConflictException();
	}

}
