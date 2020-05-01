package suite.nntp;

import primal.MoreVerbs.Split;
import primal.Verbs.Equals;
import primal.Verbs.ReadLine;
import primal.io.WriteStream;
import primal.parser.Commands;
import suite.os.Listen;
import suite.util.RunUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static primal.statics.Fail.fail;

public class NntpServerMain {

	private enum NntpCommand {
		ARTICLE, BODY, GROUP, HEAD, LIST, LISTGROUP, NEWNEWS, POST,
	}

	private Nntp nntp;

	public static void main(String[] args) {
		RunUtil.run(new NntpServerMain()::run);
	}

	private boolean run() {
		new Listen().io(119, (sis, sos) -> new Server().serve(sis, sos));
		return true;
	}

	private class Server {
		private void serve(InputStream sis, OutputStream sos) throws IOException {
			WriteStream.of(sos).doPrintWriter(pw -> {
				String currentGroupId = null;
				Map<String, String> article;
				String line;

				while (!(line = ReadLine.from(sis)).isEmpty()) {
					var pair = new Commands<>(NntpCommand.values()).recognize(line.toUpperCase());
					var options = pair.v;

					switch (pair.k) {
					case ARTICLE:
						if ((article = nntp.getArticle(currentGroupId, options)) != null) {
							pw.println("220 Okay");
							printHead(pw, article);
							pw.println();
							pw.println(article.get(Nntp.contentKey));
							pw.println(".");
						} else
							pw.println("423 Error");
						break;
					case BODY:
						if ((article = nntp.getArticle(currentGroupId, options)) != null) {
							pw.println("222 Okay");
							pw.println(article.get(Nntp.contentKey));
							pw.println(".");
						} else
							pw.println("423 Error");
						break;
					case GROUP:
						currentGroupId = options;
						break;
					case HEAD:
						if ((article = nntp.getArticle(currentGroupId, options)) != null) {
							pw.println("221 Okay");
							printHead(pw, article);
							pw.println(".");
						} else
							pw.println("423 Bad article number");
						break;
					case LIST:
						if (Equals.string(options, "ACTIVE")) {
							pw.println("215 Okay");
							for (var groupId : nntp.listGroupIds()) {
								var articleIdRange = nntp.getArticleIdRange(groupId);
								pw.println(groupId + " " + articleIdRange.k + " " + articleIdRange.v + " y");
							}
							pw.println(".");
						} else if (Equals.string(options, "NEWSGROUPS")) {
							pw.println("215 Okay");
							for (var group : nntp.listGroupIds())
								pw.println(group + " " + group);
							pw.println(".");
						} else
							fail("unrecognized LIST command " + line);
						break;
					case LISTGROUP:
						pw.println("211 Okay");
						for (var articleId : nntp.listArticleIds(currentGroupId, 0))
							pw.println(articleId);
						pw.println(".");
						break;
					case NEWNEWS:
						break;
					case POST:
						pw.println("340 Okay");
						var size = 0;
						var lines = new ArrayList<String>();
						while (!Equals.string(line = ReadLine.from(sis), ".") && size < 1048576) {
							lines.add(line);
							size += line.length();
						}
						article = new HashMap<>();
						var pos = 0;
						while (!(line = lines.get(pos++)).isEmpty())
							Split.strl(line, ":").map(article::put);
						var sb = new StringBuilder();
						while (pos < lines.size())
							sb.append(lines.get(pos++));
						article.put(Nntp.contentKey, sb.toString());
						nntp.createArticle(currentGroupId, article);
						pw.println("240 Okay");
						break;
					default:
						fail("unrecognized command " + line);
					}
				}
			});
		}

		private void printHead(PrintWriter pw, Map<String, String> article) {
			for (var e : article.entrySet()) {
				var key = e.getKey();
				if (!Equals.string(key, Nntp.contentKey))
					pw.println(key + ": " + e.getValue());
			}
		}
	}

}
