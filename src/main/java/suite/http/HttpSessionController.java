package suite.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;

import suite.http.HttpServer.Handler;
import suite.util.FileUtil;
import suite.util.HtmlUtil;
import suite.util.Util;

public class HttpSessionController {

	public static final long TIMEOUTDURATION = 3600 * 1000l;

	private Authenticator authenticator;
	private SessionManager sessionManager = new HttpSessionManager();
	private Random random = new SecureRandom();

	public interface Authenticator {
		public boolean authenticate(String username, String password);
	}

	public interface SessionManager {
		public Session get(String id);

		public void put(String id, Session session);

		public void remove(String id);
	}

	public class Session {
		private String username;
		private long lastRequestDt;

		public String getUsername() {
			return username;
		}

		public long getLastRequestDt() {
			return lastRequestDt;
		}
	}

	public HttpSessionController(Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	public HttpSessionHandler getSessionHandler(Handler handler) {
		return new HttpSessionHandler(handler);

	}

	private class HttpSessionHandler implements Handler {
		private Handler protectedHandler;

		public HttpSessionHandler(Handler protectedHandler) {
			this.protectedHandler = protectedHandler;
		}

		public void handle(HttpRequest request, HttpResponse response) throws IOException {
			long current = System.currentTimeMillis();
			String cookie = request.getHeaders().get("Cookie");
			String sessionId = cookie != null ? HttpUtil.getCookieAttrs(cookie).get("session") : null;
			Session session = sessionId != null ? sessionManager.get(sessionId) : null;

			if (Util.stringEquals(request.getPath(), "/login")) {
				Map<String, String> attrs = HttpUtil.getPostedAttrs(request.getInputStream());
				String username = attrs.get("username");
				String password = attrs.get("password");
				String path = attrs.get("path");

				if (authenticator.authenticate(username, password)) {
					sessionId = generateRandomSessionId();

					session = new Session();
					session.username = username;
					session.lastRequestDt = current;

					sessionManager.put(sessionId, session);

					HttpRequest request1 = new HttpRequest(request.getMethod() //
							, request.getServer() //
							, path //
							, request.getQuery() //
							, request.getHeaders() //
							, request.getInputStream());

					showProtectedPage(sessionId, request1, response);
				} else
					showLoginPage(response.getOutputStream(), path, true);
			} else if (Util.stringEquals(request.getPath(), "/logout")) {
				if (sessionId != null)
					sessionManager.remove(sessionId);

				showLoginPage(response.getOutputStream(), "/", false);
			} else if (session != null && current < session.lastRequestDt + TIMEOUTDURATION) {
				session.lastRequestDt = current;
				showProtectedPage(sessionId, request, response);
			} else
				showLoginPage(response.getOutputStream(), request.getPath(), false);
		}

		private void showProtectedPage(String sessionId, HttpRequest request, HttpResponse response) throws IOException {
			response.getHeaders().put("Set-Cookie", "session=" + sessionId + "; Path=/");

			protectedHandler.handle(request, response);
		}

		private void showLoginPage(OutputStream os //
				, String redirectPath //
				, boolean isLoginFailed) throws IOException {
			OutputStreamWriter writer = new OutputStreamWriter(os, FileUtil.charset);

			writer.write("<html>" //
					+ "<head><title>Login</title></head>" //
					+ "<body>" //
					+ "<font face=\"Monospac821 BT,Monaco,Consolas\">" //
					+ (isLoginFailed ? "<b>LOGIN FAILED</b><p/>" : "") //
					+ "<form name=\"login\" action=\"/login\" method=\"post\">" //
					+ "Username <input type=\"text\" name=\"username\" /><br/>" //
					+ "Password <input type=\"password\" name=\"password\" /><br/>" //
					+ "<input type=\"hidden\" name=\"path\" value=\"" + HtmlUtil.encode(redirectPath) + "\" />" //
					+ "<input type=\"submit\" value=\"Login\">" //
					+ "</form>" //
					+ "</font>" //
					+ "</body>" //
					+ "</html>");

			writer.flush();
		}
	}

	private String generateRandomSessionId() {
		byte bytes[] = new byte[16];
		random.nextBytes(bytes);

		StringBuilder sb = new StringBuilder();
		for (byte b : bytes)
			sb.append(String.format("%02x", b));

		return sb.toString();
	}

}
