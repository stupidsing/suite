package suite.text;

import java.util.List;

import suite.util.FunUtil.Fun;

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

	public static Fun<String, String> preprocessor(Fun<String, List<Run>> fun) {
		return s -> {
			StringBuilder sb = new StringBuilder();
			for (Run run : fun.apply(s))
				if (run.segment != null)
					sb.append(s.substring(run.segment.start, run.segment.end));
				else
					sb.append(run.text);
			return sb.toString();
		};
	}

}
