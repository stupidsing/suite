package suite.text;

import java.util.List;

import suite.adt.pair.Pair;
import suite.util.FunUtil.Fun;

public class Preprocess {

	public static class Run {
		public final Segment segment;
		public final String text;

		public Run(int start, int end) {
			segment = Segment.of(start, end);
			text = null;
		}

		public Run(String text) {
			segment = null;
			this.text = text;
		}
	}

	public interface Reverser {
		public int reverse(int position);
	}

	public static Pair<String, Reverser> transform(List<Fun<String, List<Run>>> funs, String in) {
		String fwd = in;
		Reverser rev = position -> position;

		for (Fun<String, List<Run>> fun : funs) {
			Reverser rev0 = rev;
			List<Run> runs = fun.apply(fwd);
			fwd = forward(fwd, runs);
			rev = position -> rev0.reverse(reverse(runs, position));
		}

		return Pair.of(fwd, rev);
	}

	private static String forward(String in, List<Run> runs) {
		StringBuilder sb = new StringBuilder();
		for (Run run : runs)
			if (run.segment != null)
				sb.append(in.substring(run.segment.start, run.segment.end));
			else
				sb.append(run.text);
		return sb.toString();
	}

	private static int reverse(List<Run> runs, int targetPosition) {
		int sourcePosition = 0;
		for (Run run : runs) {
			Segment segment = run.segment;
			if (segment != null) {
				int runLength = segment.end - segment.start;
				if (runLength <= targetPosition) {
					sourcePosition = segment.start + runLength;
					targetPosition -= runLength;
				} else
					return segment.start + targetPosition;
			} else {
				int runLength = run.text.length();
				if (runLength <= targetPosition)
					targetPosition -= runLength;
				else
					return sourcePosition;
			}
		}
		return sourcePosition;
	}

}
