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
		var fwd = in;
		Reverser rev = position -> position;

		for (Fun<String, List<Run>> fun : funs) {
			var rev0 = rev;
			List<Run> runs = fun.apply(fwd);
			fwd = forward(fwd, runs);
			rev = position -> rev0.reverse(reverse(runs, position));
		}

		return Pair.of(fwd, rev);
	}

	private static String forward(String in, List<Run> runs) {
		var sb = new StringBuilder();
		for (var run : runs)
			if (run.segment != null)
				sb.append(in.substring(run.segment.start, run.segment.end));
			else
				sb.append(run.text);
		return sb.toString();
	}

	private static int reverse(List<Run> runs, int targetPosition) {
		var sourcePosition = 0;
		for (var run : runs) {
			var segment = run.segment;
			if (segment != null) {
				var runLength = segment.end - segment.start;
				if (runLength <= targetPosition) {
					sourcePosition = segment.start + runLength;
					targetPosition -= runLength;
				} else
					return segment.start + targetPosition;
			} else {
				var runLength = run.text.length();
				if (runLength <= targetPosition)
					targetPosition -= runLength;
				else
					return sourcePosition;
			}
		}
		return sourcePosition;
	}

}
