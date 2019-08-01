package suite.parser;

import primal.Verbs.Build;
import primal.fp.Funs.Iterate;

/**
 * Render template into pages.
 *
 * @author ywsing
 */
public class TemplateRenderer implements Iterate<String> {

	public static String openTemplate = "<#";
	public static String closeTemplate = "#>";

	private Iterate<String> wrapText;
	private Iterate<String> wrapExpression;

	public TemplateRenderer(Iterate<String> wrapText, Iterate<String> wrapExpression) {
		this.wrapText = wrapText;
		this.wrapExpression = wrapExpression;
	}

	@Override
	public String apply(String in) {
		return Build.string(sb -> {
			var start = 0;

			while (true) {
				var pos0 = in.indexOf(openTemplate, start);
				if (pos0 == -1)
					break;
				var pos1 = pos0 + openTemplate.length();

				var pos2 = in.indexOf(closeTemplate, pos1);
				if (pos2 == -1)
					break;
				var pos3 = pos2 + closeTemplate.length();

				sb.append(wrapText.apply(in.substring(start, pos0)));
				sb.append(wrapExpression.apply(in.substring(pos1, pos2)));
				start = pos3;
			}

			sb.append(wrapText.apply(in.substring(start)));
		});
	}

}
