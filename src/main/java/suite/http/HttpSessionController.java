package suite.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import suite.http.HttpServer.Handler;
import suite.util.FileUtil;
import suite.util.HtmlUtil;
import suite.util.Util;

public class HttpSessionController {

	private Authenticator authenticator;
	private SessionManager sessionManager;

	public interface Authenticator {
		public boolean authenticate(String username, String password);
	}

	public interface SessionManager {
		public Session get(String id);

		public void put(String id, Session session);
	}

	public class Session {
		private String username;
		private long lastRequestDt;
	}

	public class HttpSessionHandler implements Handler {
		private Handler protectedHandler;

		public HttpSessionHandler(Handler protectedHandler) {
			this.protectedHandler = protectedHandler;
		}

		public void handle(HttpRequest request, HttpResponse response) throws IOException {
			if (request.getHeaders().get("Cookie") != null) {
				// TODO retrieve session

				// TODO check if session outdated
				// TODO update session timestamp
			} else if (Util.equals(request.getPath(), "/login")) {
				Map<String, String> attrs = HttpUtil.getPostedAttrs(request.getInputStream());
				String username = attrs.get("username");
				String password = attrs.get("password");
				String url = attrs.get("url");

				if (authenticator.authenticate(username, password)) {
					// TODO add session
					// TODO sign a cookie
					// TODO set cookie
					response.getHeaders().put("Set-Cookie", "session=???; Path=/");
					protectedHandler.handle(request, response);
				} else
					showLoginPage(response.getOutputStream(), url, true);
			} else
				showLoginPage(response.getOutputStream(), request.getPath(), false);
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
