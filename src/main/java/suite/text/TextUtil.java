package suite.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.lcs.Lccs;
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
			this.patchDataSegments = new ArrayList<>(patchDataSegments);
		}

		public void next() {
			PatchDataSegment pds = patchDataSegments.get(pos);
			offset += pds.getDataSegmentNew().length() - pds.getDataSegmentOrg().length();
			pos++;
		}
	}

	public PatchData diff(Bytes bytesOrg, Bytes bytesNew) {
		Lccs lccs = new Lccs();
		Pair<Segment, Segment> diff = lccs.lccs(bytesOrg, bytesNew);
		Segment so = diff.t0, sn = diff.t1;
		int o0 = 0, o1 = so.getStart(), o2 = so.getEnd();
		int n0 = 0, n1 = sn.getStart(), n2 = sn.getEnd();

		if (!so.isEmpty() && !sn.isEmpty()) {
			PatchDataSegment pds0 = new PatchDataSegment(o0, n0 //
					, bytesOrg.subbytes(o0, o1) //
					, bytesNew.subbytes(n0, n1));

			PatchDataSegment pds1 = new PatchDataSegment(o1, n1 //
					, bytesOrg.subbytes(o1, o2) //
			);

			PatchDataSegment pds2 = new PatchDataSegment(o2, n2 //
					, bytesOrg.subbytes(o2) //
					, bytesNew.subbytes(n2));

			List<PatchDataSegment> pdsList = new ArrayList<>();
			pdsList.addAll(diff(pds0).getPatchDataSegments());
			pdsList.add(pds1);
			pdsList.addAll(diff(pds2).getPatchDataSegments());

			return new PatchData(pdsList);
		} else
			return new PatchData(Arrays.asList( //
					new PatchDataSegment(0, 0, bytesOrg, bytesNew)));
	}

	private PatchData diff(PatchDataSegment patchDataSegment) {
		DataSegment dsOrg = patchDataSegment.getDataSegmentOrg();
		DataSegment dsNew = patchDataSegment.getDataSegmentNew();
		PatchData subPatchData = diff(dsOrg.getBytes(), dsNew.getBytes());
		List<PatchDataSegment> pdsList = new ArrayList<>();

		for (PatchDataSegment pds : subPatchData.getPatchDataSegments())
			pdsList.add(pds.adjust(dsOrg.getStart(), dsNew.getStart()));

		return new PatchData(pdsList);
	}

	public Bytes patch(Bytes bytes, PatchData patchData) {
		BytesBuilder bytesBuilder = new BytesBuilder();

		for (PatchDataSegment pds : patchData)
			if (!pds.isChanged()) {
				DataSegment dso = pds.getDataSegmentOrg();
				bytesBuilder.append(bytes.subbytes(dso.getStart(), dso.getEnd()));
			} else
				bytesBuilder.append(pds.getDataSegmentNew().getBytes());

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
				int endx = pdsx.getDataSegmentOrg().getEnd();
				int endy = pdsy.getDataSegmentOrg().getEnd();

				if (endx == endy)
					isAdvanceX = !pdsy.isChanged();
				else
					isAdvanceX = endx < endy;
			} else
				isAdvanceX = isAvailX;

			start = isAdvanceX ? advance(mdx, mdy, start, merged) : advance(mdy, mdx, start, merged);
		}

		return new PatchData(merged);
	}

	private int advance(MergeData mdx, MergeData mdy, int start, List<PatchDataSegment> pdsList) throws ConflictException {
		PatchDataSegment pdsx = mdx.patchDataSegments.get(mdx.pos);
		PatchDataSegment pdsy = mdy.patchDataSegments.get(mdy.pos);
		DataSegment dsxOrg = pdsx.getDataSegmentOrg(), dsxNew = pdsx.getDataSegmentNew();
		DataSegment dsyOrg = pdsy.getDataSegmentOrg(), dsyNew = pdsy.getDataSegmentNew();

		// If both patches do not overlap in original sections,
		// they can be merged
		boolean isSeparate = dsxOrg.getEnd() <= dsyOrg.getStart();

		// If the longer patch segment is not changing anything,
		// they can be merged
		boolean isUnchanged = !pdsy.isChanged();

		// If both patches have same starting (head) content in original and
		// target sections, the head parts can be merged
		int dsxOrgLength = dsxOrg.length(), dsxNewLength = dsxNew.length();
		int dsyOrgLength = dsyOrg.length(), dsyNewLength = dsyNew.length();
		boolean isMappingsAgree = dsxOrg.getStart() == dsyOrg.getStart()
				&& dsxOrgLength <= dsyOrgLength //
				&& dsxNewLength <= dsyNewLength //
				&& Util.equals(dsxOrg.getBytes(), dsyOrg.getBytes().subbytes(0, dsxOrgLength))
				&& Util.equals(dsxNew.getBytes(), dsyNew.getBytes().subbytes(0, dsyOrgLength));

		if (isSeparate || isUnchanged || isMappingsAgree) {
			DataSegment dsOrg1 = dsxOrg.right(start);
			DataSegment dsNew1 = dsxNew.right(start + mdx.offset);
			pdsList.add(new PatchDataSegment(dsOrg1, dsNew1.adjust(mdy.offset)));

			// If only the head part can merge, add back tail parts to the lists
			if (isMappingsAgree) {
				DataSegment dsyOrg0 = dsyOrg.left(dsxOrg.getEnd());
				DataSegment dsyNew0 = dsyNew.left(dsyNew.getStart() + dsxNewLength);
				DataSegment dsyOrg1 = dsyOrg.right(dsxOrg.getEnd());
				DataSegment dsyNew1 = dsyNew.right(dsyNew.getStart() + dsxNewLength);
				mdy.patchDataSegments.set(mdy.pos, new PatchDataSegment(dsyOrg0, dsyNew0));
				mdy.next();
				mdy.patchDataSegments.add(mdy.pos, new PatchDataSegment(dsyOrg1, dsyNew1));
			}

			mdx.next();
			return dsxOrg.getEnd();
		} else
			throw new ConflictException();
	}

}
