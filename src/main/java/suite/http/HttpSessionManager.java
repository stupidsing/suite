package suite.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import suite.http.HttpServer.Handler;
import suite.util.FileUtil;
import suite.util.HtmlUtil;
import suite.util.Util;

public class HttpSessionManager {

	private Authenticator authenticator;

	public interface Authenticator {
		public boolean authenticate(String username, String password);
	}

	public class HttpSessionHandler implements Handler {
		private Handler parent; // Protected by session

		public HttpSessionHandler(Handler parent) {
			this.parent = parent;
		}

		public void handle(String method //
				, String server //
				, String path //
				, String query //
				, Map<String, String> headers //
				, InputStream is //
				, OutputStream os) throws IOException {
			if (headers.containsKey("Cookie")) {
				// TODO check if session outdated
				// TODO update session timestamp
			} else if (Util.equals(path, "/login")) {
				// TODO if too many sessions, remove outdated sessions
				// TODO authenticate user

				int size = 4096;
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();
				char buffer[] = new char[size];
				int nCharsRead;

				while ((nCharsRead = br.read(buffer)) >= 0)
					sb.append(buffer, 0, nCharsRead);

				Map<String, String> attrs = HttpUtil.getAttrs(sb.toString());
				String username = attrs.get("username");
				String password = attrs.get("password");
				String url = attrs.get("url");

				if (authenticator.authenticate(username, password))
					parent.handle(method, server, path, query, headers, is, os);
				else
					showLoginPage(os, url, true);
			} else
				showLoginPage(os, path, false);
		}

		private void showLoginPage(OutputStream os //
				, String redirectUrl //
				, boolean isLoginFailed) throws IOException {
			try (Writer writer = new OutputStreamWriter(os, FileUtil.charset)) {
				writer.write("<html>" //
						+ "<head><title>Login</title></head>" //
						+ "<body>" //
						+ "<font face=\"Monospac821 BT,Monaco,Consolas\">" //
						+ (isLoginFailed ? "<b>LOGIN FAILED</b><p/>" : "") //
						+ "<form name=\"login\" action=\"/login\" method=\"get\">" //
						+ "Username <input type=\"text\" name=\"username\" />" //
						+ "Password <input type=\"password\" name=\"password\" />" //
						+ "<input type=\"hidden\" name=\"url\" value=\"" + HtmlUtil.encode(redirectUrl) + "\" />" //
						+ "<input type=\"submit\" value=\"Login\">" //
						+ "</form>" //
						+ "</font>" //
						+ "</body>" //
						+ "</html>");
			}
		}
	}

}
