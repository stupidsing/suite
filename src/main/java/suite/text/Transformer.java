package suite.text;

import java.util.List;

public class Transformer {

	public static class Run {
		public final Segment segment;
		public final String text;

		public Run(int start, int end) {
			this.segment = new Segment(start, end);
			this.text = null;
		}

		public Run(String text) {
			this.segment = null;
			this.text = text;
		}
	}

	public String combineRuns(String in, List<Run> runs) {
		StringBuilder sb = new StringBuilder();
		for (Run run : runs)
			if (run.segment != null)
				sb.append(in.substring(run.segment.start, run.segment.end));
			else
				sb.append(run.text);
		return sb.toString();
	}

}
