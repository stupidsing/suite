package suite.http;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;

import suite.immutable.IList;
import suite.primitive.LngMutable;
import suite.streamlet.As;
import suite.util.HtmlUtil;
import suite.util.To;

public class HttpSessionControl {

	public static long TIMEOUTDURATION = 3600 * 1000l;

	private Authenticator authenticator;
	private HtmlUtil htmlUtil = new HtmlUtil();
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
		public final String username;
		public final LngMutable lastRequestDt;

		private Session(String username, long current) {
			this.username = username;
			lastRequestDt = LngMutable.of(current);
		}
	}

	public HttpSessionControl(Authenticator authenticator) {
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
			var session = sessionId != null ? sessionManager.get(sessionId) : null;
			HttpResponse response;

			if (Objects.equals(request.paths, IList.of("login"))) {
				var attrs = HttpHeaderUtil.getPostedAttrs(request.inputStream);
				var username = attrs.get("username");
				var password = attrs.get("password");
				var paths = HttpHeaderUtil.getPaths(attrs.get("path"));

				if (authenticator.authenticate(username, password)) {
					sessionManager.put(sessionId = generateRandomSessionId(), session = new Session(username, current));

					var request1 = new HttpRequest( //
							request.method, //
							request.server, //
							paths, //
							request.query, //
							request.headers, //
							request.inputStream);

					response = showProtectedPage(request1, sessionId);
				} else
					response = showLoginPage(paths, true);
			} else if (Objects.equals(request.paths, IList.of("logout"))) {
				if (sessionId != null)
					sessionManager.remove(sessionId);

				response = showLoginPage(IList.end(), false);
			} else if (session != null && current < session.lastRequestDt.get() + TIMEOUTDURATION) {
				session.lastRequestDt.update(current);
				response = showProtectedPage(request, sessionId);
			} else
				response = showLoginPage(request.paths, false);

			return response;
		}

		private HttpResponse showProtectedPage(HttpRequest request, String sessionId) {
			var r = protectedHandler.handle(request);
			var headers1 = r.headers.put("Set-Cookie", "session=" + sessionId + "; Path=/site");
			return new HttpResponse(r.status, headers1, r.out);
		}

		private HttpResponse showLoginPage(IList<String> redirectPath, boolean isLoginFailed) {
			var redirectPath1 = redirectPath.streamlet().map(p -> "/" + p).collect(As::joined);

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
