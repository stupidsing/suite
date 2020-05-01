package suite.http;

import primal.MoreVerbs.Pull;
import primal.Verbs.Build;
import primal.Verbs.Equals;
import primal.persistent.PerList;
import suite.http.Http.*;
import suite.util.HtmlUtil;

import java.security.SecureRandom;
import java.util.Random;
import java.util.function.BiPredicate;

/**
 * Cookie-based HTTP authentication.
 *
 * @author ywsing
 */
public class HttpAuthSession {

	public static long timeoutDuration = 3600 * 1000l;

	private HtmlUtil htmlUtil = new HtmlUtil();
	private SessionManager sm = new HttpSessionManager();
	private Random random = new SecureRandom();

	public Handler getHandler(BiPredicate<String, String> authenticate, Handler protectedHandler) {
		return new Handler() {
			public Response handle(Request request) {
				var current = System.currentTimeMillis();

				var sessionIdOpt = request
						.headers
								.getOpt("Cookie")
								.map(cookie -> HttpHeaderUtil.getCookieAttrs(cookie).get("session"));

				var session = sessionIdOpt.map(sm::get).or(null);
				Response response;

				if (Equals.ab(request.paths, PerList.of("login"))) {
					var attrs = HttpHeaderUtil.getPostedAttrs(request.in);
					var username = attrs.get("username");
					var password = attrs.get("password");
					var paths = HttpHeaderUtil.getPaths(attrs.get("path"));

					if (authenticate.test(username, password)) {
						var sessionId = getRandomSessionId();
						sm.put(sessionId, session = new Session(username, current));

						var request1 = new Request(
								request.method,
								request.server,
								paths,
								request.query,
								request.headers,
								request.in);

						response = showProtectedPage(request1, sessionId);
					} else
						response = showLoginPage(paths, true);
				} else if (Equals.ab(request.paths, PerList.of("logout"))) {
					sessionIdOpt.sink(sm::remove);
					response = showLoginPage(PerList.end(), false);
				} else if (session != null && current < session.lastRequestDt.value() + timeoutDuration) {
					session.lastRequestDt.update(current);
					response = showProtectedPage(request, sessionIdOpt.g());
				} else
					response = showLoginPage(request.paths, false);

				return response;
			}

			private Response showProtectedPage(Request request, String sessionId) {
				var r = protectedHandler.handle(request);
				var headers1 = r.headers.put("Set-Cookie", "session=" + sessionId + "; Path=/site");
				return new Response(r.status, headers1, r.body);
			}

			private Response showLoginPage(PerList<String> redirectPath, boolean isLoginFailed) {
				var redirectPath1 = redirectPath.streamlet().map(p -> "/" + p).toJoinedString();

				return Response.of(Pull.from("<html>"
						+ "<head><title>Login</title></head>"
						+ "<body>"
						+ "<font face=\"Monospac821 BT,Monaco,Consolas\">"
						+ (isLoginFailed ? "<b>LOGIN FAILED</b><p/>" : "")
						+ "<form name=\"login\" action=\"login\" method=\"post\">"
						+ "Username <input type=\"text\" name=\"username\" autofocus /><br/>"
						+ "Password <input type=\"password\" name=\"password\" /><br/>"
						+ "<input type=\"hidden\" name=\"path\" value=\"" + htmlUtil.encode(redirectPath1) + "\" />"
						+ "<input type=\"submit\" value=\"Login\">"
						+ "</form>"
						+ "</font>"
						+ "</body>"
						+ "</html>"));
			}

			private String getRandomSessionId() {
				var bytes = new byte[16];
				random.nextBytes(bytes);

				return Build.string(sb -> {
					for (var b : bytes)
						sb.append(String.format("%02x", b));
				});
			}
		};
	}

}
