package suite.text;

import java.util.List;

import suite.net.Bytes;
import suite.net.Bytes.BytesBuilder;

public class Patch {

	public Bytes patch(Bytes bytes, List<DiffSegment> diffSegments) {
		BytesBuilder bytesBuilder = new BytesBuilder();

		for (DiffSegment diffSegment : diffSegments)
			if (!diffSegment.isChanged()) {
				Segment segmentAye = diffSegment.getSegmentAye();
				bytesBuilder.append(bytes.subbytes(segmentAye.getStart(), segmentAye.getEnd()));
			} else
				bytesBuilder.append(diffSegment.getSegmentBee().getBytes());

		return bytesBuilder.toBytes();
	}

}
