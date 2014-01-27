package suite.text;

import java.util.List;

import suite.net.Bytes;
import suite.net.Bytes.BytesBuilder;

public class Patch {

	public Bytes patch(Bytes bytes, List<DiffSegment> diffSegments) {
		BytesBuilder bytesBuilder = new BytesBuilder();

		for (DiffSegment diffSegment : diffSegments)
			if (diffSegment.isChanged())
				bytesBuilder.append(diffSegment.getSegmentBee().getBytes());
			else
				bytesBuilder.append(bytes.subbytes(diffSegment.getSegmentAye().getStart(), diffSegment.getSegmentAye().getEnd()));

		return bytesBuilder.toBytes();
	}

}
