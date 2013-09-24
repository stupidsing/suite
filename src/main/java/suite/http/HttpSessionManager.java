package suite.http;

import java.io.IOException;
import java.io.InputStream;
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
		private Handler protectedHandler;

		public HttpSessionHandler(Handler protectedHandler) {
			this.protectedHandler = protectedHandler;
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
				Map<String, String> attrs = HttpUtil.getPostedAttrs(is);
				String username = attrs.get("username");
				String password = attrs.get("password");
				String url = attrs.get("url");

				if (authenticator.authenticate(username, password)) {
					// TODO add session
					protectedHandler.handle(method, server, path, query, headers, is, os);
				} else
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
