package suite.smtp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import primal.MoreVerbs.Read;
import primal.Nouns.Utf8;
import primal.Verbs.Build;
import primal.Verbs.Equals;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.os.Log_;
import suite.cfg.HomeDir;
import suite.os.Listen;

public class SmtpServer {

	private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ENGLISH);
	private Listen listen = new Listen();

	private String me = "pointless.online";
	private int size = 262144;

	public void serve() {
		listen.ioAsync(25, (is, os, close) -> {
			var mail = new Object() {
				private String from;
				private List<String> tos = new ArrayList<>();
				private String data;
			};

			try (var isr = new InputStreamReader(is, Utf8.charset);
					var osw = new OutputStreamWriter(os, Utf8.charset);
					var br = new BufferedReader(isr);
					var bw = new BufferedWriter(osw);) {
				Source<String> read = () -> {
					try {
						var line = br.readLine();
						Log_.info("< " + line);
						return line;
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				};

				Sink<String> write = line -> {
					try {
						Log_.info("> " + line);
						bw.write(line + "\r\n");
						bw.flush();
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				};

				write.f("220 ready");
				String line;

				while ((line = read.g()) != null)
					if (line.startsWith("DATA")) {
						write.f("354 come on");

						mail.data = Build.string(sb -> {
							String line_;
							while (!Equals.string(line_ = read.g(), "."))
								sb.append(line_ + "\n");
						});

						var contents = Build.string(sb -> {
							sb.append(mail.from + " -> " + Read.from(mail.tos).toJoinedString(",") + "\n");
							sb.append("\n" + mail.data + "\n");
						});

						for (var to : mail.tos)
							if (to.endsWith("@" + me)) {
								var path = HomeDir.resolve(to).resolve(dtf.format(LocalDateTime.now()));
								Files.writeString(path, contents);
							}

						write.f("250 ok");
					} else if (line.startsWith("EHLO")) {
						write.f("250-" + me + " hello " + line.substring(5));
						write.f("250 SIZE " + size);
					} else if (line.startsWith("HELO"))
						write.f("250 hello " + line.substring(5));
					else if (line.startsWith("MAIL FROM")) {
						mail.from = unquote(line.substring(10));
						write.f("250 ok");
					} else if (line.startsWith("NOOP"))
						write.f("250 ok");
					else if (line.startsWith("QUIT")) {
						write.f("221 done");
						break;
					} else if (line.startsWith("RCPT TO")) {
						mail.tos.add(unquote(line.substring(8)));
						write.f("250 ok");
					} else if (line.startsWith("RSET"))
						mail.tos.clear();
					else if (line.startsWith("SIZE"))
						write.f("250 SIZE " + size);
					else if (line.startsWith("VRFY"))
						write.f("250 " + line.substring(5));
					else
						throw new RuntimeException();
			}
		});
	}

	private String unquote(String email) {
		return email.startsWith("<") && email.endsWith(">") ? email.substring(1, email.length() - 1) : email;
	}

}
