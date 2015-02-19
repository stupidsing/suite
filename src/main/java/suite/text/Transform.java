package suite.text;

import java.util.List;

import suite.util.FunUtil.Fun;
import suite.util.Pair;

public class Transform {

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

	public static Pair<String, Fun<Integer, Integer>> transform(List<Fun<String, List<Run>>> funs, String in) {
		String fwd = in;
		Fun<Integer, Integer> rev = pos -> pos;
		for (Fun<String, List<Run>> fun : funs) {
			Fun<Integer, Integer> rev0 = rev;
			List<Run> runs = fun.apply(fwd);
			fwd = forward(fwd, runs);
			rev = pos -> rev0.apply(reverse(runs, pos));
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
				if (targetPosition >= runLength) {
					sourcePosition = segment.start + runLength;
					targetPosition -= runLength;
				} else
					return segment.start + targetPosition;
			} else {
				int runLength = run.text.length();
				if (targetPosition >= runLength)
					targetPosition -= runLength;
				else
					return sourcePosition;
			}
		}
		return sourcePosition;
	}

}
