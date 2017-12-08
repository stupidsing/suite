package suite.http;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import suite.immutable.IList;
import suite.immutable.IMap;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.HtmlUtil;
import suite.util.To;

public class HttpSessionController {

	public static long TIMEOUTDURATION = 3600 * 1000l;

	private Authenticator authenticator;
	private SessionManager sessionManager = new HttpSessionManager();
	private Random random = new SecureRandom();
	private HtmlUtil htmlUtil = new HtmlUtil();

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

	public HttpSessionHandler getSessionHandler(HttpHandler handler) {
		return new HttpSessionHandler(handler);

	}

	private class HttpSessionHandler implements HttpHandler {
		private HttpHandler protectedHandler;

		public HttpSessionHandler(HttpHandler protectedHandler) {
			this.protectedHandler = protectedHandler;
		}

		public HttpResponse handle(HttpRequest request) {
			long current = System.currentTimeMillis();
			String cookie = request.headers.get("Cookie");
			String sessionId = cookie != null ? HttpHeaderUtil.getCookieAttrs(cookie).get("session") : null;
			Session session = sessionId != null ? sessionManager.get(sessionId) : null;
			HttpResponse response;

			if (Objects.equals(request.path, IList.of("login"))) {
				Map<String, String> attrs = HttpHeaderUtil.getPostedAttrs(request.inputStream);
				String username = attrs.get("username");
				String password = attrs.get("password");
				IList<String> path = HttpHeaderUtil.getPath(attrs.get("path"));

				if (authenticator.authenticate(username, password)) {
					sessionId = generateRandomSessionId();

					session = new Session();
					session.username = username;
					session.lastRequestDt = current;

					sessionManager.put(sessionId, session);

					HttpRequest request1 = new HttpRequest( //
							request.method, //
							request.server, //
							path, //
							request.query, //
							request.headers, //
							request.inputStream);

					response = showProtectedPage(request1, sessionId);
				} else
					response = showLoginPage(path, true);
			} else if (Objects.equals(request.path, IList.of("logout"))) {
				if (sessionId != null)
					sessionManager.remove(sessionId);

				response = showLoginPage(IList.end(), false);
			} else if (session != null && current < session.lastRequestDt + TIMEOUTDURATION) {
				session.lastRequestDt = current;
				response = showProtectedPage(request, sessionId);
			} else
				response = showLoginPage(request.path, false);

			return response;
		}

		private HttpResponse showProtectedPage(HttpRequest request, String sessionId) {
			HttpResponse r = protectedHandler.handle(request);
			IMap<String, String> headers1 = r.headers.put("Set-Cookie", "session=" + sessionId + "; Path=/site");
			return new HttpResponse(r.status, headers1, r.out);
		}

		private HttpResponse showLoginPage(IList<String> redirectPath, boolean isLoginFailed) {
			String redirectPath1 = Read.from(redirectPath).map(p -> "/" + p).collect(As::joined);

			return HttpResponse.of(To.outlet("<html>" //
					+ "<head><title>Login</title></head>" //
					+ "<body>" //
					+ "<font face=\"Monospac821 BT,Monaco,Consolas\">" //
					+ (isLoginFailed ? "<b>LOGIN FAILED</b><p/>" : "") //
					+ "<form name=\"login\" action=\"login\" method=\"post\">" //
					+ "Username <input type=\"text\" name=\"username\" autofocus /><br/>" //
					+ "Password <input type=\"password\" name=\"password\" /><br/>" //
					+ "<input type=\"hidden\" name=\"path\" value=\"" + htmlUtil.encode(redirectPath1) + "\" />" //
					+ "<input type=\"submit\" value=\"Login\">" //
					+ "</form>" //
					+ "</font>" //
					+ "</body>" //
					+ "</html>"));
		}
	}

	private String generateRandomSessionId() {
		byte[] bytes = new byte[16];
		random.nextBytes(bytes);

		StringBuilder sb = new StringBuilder();
		for (byte b : bytes)
			sb.append(String.format("%02x", b));

		return sb.toString();
	}

}
