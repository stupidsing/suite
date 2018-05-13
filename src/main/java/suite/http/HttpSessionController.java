package suite.http;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;

import suite.immutable.IList;
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
		public final String username;
		private long lastRequestDt;

		private Session(String username) {
			this.username = username;
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
			var current = System.currentTimeMillis();
			var cookie = request.headers.get("Cookie");
			var sessionId = cookie != null ? HttpHeaderUtil.getCookieAttrs(cookie).get("session") : null;
			Session session = sessionId != null ? sessionManager.get(sessionId) : null;
			HttpResponse response;

			if (Objects.equals(request.path, IList.of("login"))) {
				var attrs = HttpHeaderUtil.getPostedAttrs(request.inputStream);
				var username = attrs.get("username");
				var password = attrs.get("password");
				var path = HttpHeaderUtil.getPath(attrs.get("path"));

				if (authenticator.authenticate(username, password)) {
					sessionId = generateRandomSessionId();

					session = new Session(username);
					session.lastRequestDt = current;

					sessionManager.put(sessionId, session);

					var request1 = new HttpRequest( //
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
			var r = protectedHandler.handle(request);
			var headers1 = r.headers.put("Set-Cookie", "session=" + sessionId + "; Path=/site");
			return new HttpResponse(r.status, headers1, r.out);
		}

		private HttpResponse showLoginPage(IList<String> redirectPath, boolean isLoginFailed) {
			var redirectPath1 = Read.from(redirectPath).map(p -> "/" + p).collect(As::joined);

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
		var bytes = new byte[16];
		random.nextBytes(bytes);

		var sb = new StringBuilder();
		for (var b : bytes)
			sb.append(String.format("%02x", b));

		return sb.toString();
	}

}
