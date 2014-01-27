package suite.text;

import java.util.ArrayList;
import java.util.List;

import suite.algo.LongestCommonContinuousSequence;
import suite.fastlist.IntList;
import suite.net.Bytes;
import suite.util.Util;

public class Diff {

	public List<DiffSegment> diff(Bytes bytesAye, Bytes bytesBee) {
		LongestCommonContinuousSequence lccs = new LongestCommonContinuousSequence();
		IntList intList = Util.first(lccs.lccs(bytesAye.getBytes(), bytesBee.getBytes()));

		DiffSegment diffSegment0 = new DiffSegment(intList //
				, bytesAye.subbytes(0, intList.get(0)) //
				, bytesBee.subbytes(0, intList.get(2)));

		DiffSegment diffSegment1 = new DiffSegment(intList //
				, bytesAye.subbytes(intList.get(0), intList.get(1)));

		DiffSegment diffSegment2 = new DiffSegment(intList //
				, bytesAye.subbytes(intList.get(1), bytesAye.size()) //
				, bytesBee.subbytes(intList.get(3), bytesBee.size()));

		List<DiffSegment> diffSegments = new ArrayList<>();
		diffSegments.addAll(diff(diffSegment0));
		diffSegments.add(diffSegment1);
		diffSegments.addAll(diff(diffSegment2));

		return diffSegments;
	}

	private List<DiffSegment> diff(DiffSegment diffSegment0) {
		Bytes bytesAye = diffSegment0.getSegmentAye().getBytes();
		Bytes bytesBee = diffSegment0.getSegmentBee().getBytes();
		List<DiffSegment> diffSegments = diff(bytesAye, bytesBee);
		List<DiffSegment> diffSegments1 = new ArrayList<>();
		int startAye = diffSegment0.getSegmentAye().getStart();
		int startBee = diffSegment0.getSegmentBee().getStart();

		for (DiffSegment diffSegment : diffSegments)
			diffSegments1.add(diffSegment.adjust(startAye, startBee));

		return diffSegments1;
	}

}
