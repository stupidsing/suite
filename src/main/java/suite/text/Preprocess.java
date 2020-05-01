package suite.text;

import primal.Verbs.Build;
import primal.adt.Pair;
import primal.fp.Funs.Fun;

import java.util.List;

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

		for (var fun : funs) {
			var rev0 = rev;
			var runs = fun.apply(fwd);
			fwd = forward(fwd, runs);
			rev = position -> rev0.reverse(reverse(runs, position));
		}

		return Pair.of(fwd, rev);
	}

	private static String forward(String in, List<Run> runs) {
		return Build.string(sb -> {
			for (var run : runs)
				sb.append(run.segment != null ? in.substring(run.segment.start, run.segment.end) : run.text);
		});
	}

	private static int reverse(List<Run> runs, int targetPosition) {
		var sourcePosition = 0;
		for (var run : runs) {
			var segment = run.segment;
			if (segment != null) {
				var runLength = segment.length();
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
