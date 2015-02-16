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
		String s = in;
		Fun<Integer, Integer> rl = pos -> pos;
		for (Fun<String, List<Run>> fun : funs) {
			Fun<Integer, Integer> rl0 = rl;
			List<Run> runs = fun.apply(s);
			s = forward(s, runs);
			rl = pos -> rl0.apply(reverse(runs, pos));
		}
		return Pair.of(s, rl);
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
					sourcePosition += runLength;
					targetPosition -= runLength;
				} else
					return sourcePosition + targetPosition;
			} else {
				int runLength = run.text.length();
				if (targetPosition >= runLength)
					sourcePosition += runLength;
				else
					return sourcePosition;
			}
		}
		return sourcePosition;
	}

}
