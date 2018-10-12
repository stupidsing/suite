package suite.nntp;

import static suite.util.Friends.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import suite.os.SocketUtil;
import suite.util.CommandUtil;
import suite.util.RunUtil;
import suite.util.String_;
import suite.util.Util;
import suite.util.WriteStream;

public class NntpServerMain {

	private enum NntpCommand {
		ARTICLE, BODY, GROUP, HEAD, LIST, LISTGROUP, NEWNEWS, POST,
	}

	private Nntp nntp;

	public static void main(String[] args) {
		RunUtil.run(() -> new NntpServerMain().run());
	}

	private boolean run() {
		new SocketUtil().listenIo(119, (sis, sos) -> new Server().serve(sis, sos));
		return true;
	}

	private class Server {
		private void serve(InputStream sis, OutputStream sos) throws IOException {
			WriteStream.of(sos).doPrintWriter(pw -> {
				String currentGroupId = null;
				Map<String, String> article;
				String line;

				while (!(line = Util.readLine(sis)).isEmpty()) {
					var pair = new CommandUtil<>(NntpCommand.values()).recognize(line.toUpperCase());
					var options = pair.t1;

					switch (pair.t0) {
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
						if (String_.equals(options, "ACTIVE")) {
							pw.println("215 Okay");
							for (var groupId : nntp.listGroupIds()) {
								var articleIdRange = nntp.getArticleIdRange(groupId);
								pw.println(groupId + " " + articleIdRange.t0 + " " + articleIdRange.t1 + " y");
							}
							pw.println(".");
						} else if (String_.equals(options, "NEWSGROUPS")) {
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
						while (!String_.equals(line = Util.readLine(sis), ".") && size < 1048576) {
							lines.add(line);
							size += line.length();
						}
						article = new HashMap<>();
						var pos = 0;
						while (!(line = lines.get(pos++)).isEmpty())
							String_.split2l(line, ":").map(article::put);
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
				if (!String_.equals(key, Nntp.contentKey))
					pw.println(key + ": " + e.getValue());
			}
		}
	}

}
