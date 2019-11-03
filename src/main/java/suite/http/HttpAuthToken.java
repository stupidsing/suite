package suite.http;

import java.util.List;

import primal.MoreVerbs.Pull;
import primal.MoreVerbs.Read;
import primal.Nouns.Utf8;
import primal.Verbs.Equals;
import primal.adt.FixieArray;
import primal.adt.Fixie_.FixieFun3;
import primal.fp.Funs2.Fun2;
import suite.cfg.Defaults;
import suite.http.Crypts.Crypt;
import suite.math.Sha2;
import suite.trade.Time;

/**
 * Token-based HTTP authentication.
 *
 * @author ywsing
 */
public class HttpAuthToken {

	public static long timeoutDuration = 3600l;

	private Crypts crypts = new Crypts();
	private Crypt<String> aes = crypts.aes(Defaults.salt);
	private Crypt<byte[]> rsa = crypts.rsaBs("");
	private Sha2 sha2 = new Sha2();

	public HttpHandler handleLogin(String authenticatePath) {
		return request -> HttpResponse.of(Pull.from("<html>" //
				+ "<head><title>Login</title></head>" //
				+ "<body>" //
				+ "<font face=\"Monospac821 BT,Monaco,Consolas\">" //
				+ "<form name=\"login\" action=\"login\" method=\"post\">" //
				+ "Username <input type=\"text\" name=\"username\" autofocus /><br/>" //
				+ "Password <input type=\"password\" name=\"password\" /><br/>" //
				+ "<input type=\"submit\" value=\"Login\">" //
				+ "</form>" //
				+ "</font>" //
				+ "</body>" //
				+ "</html>"));
	}

	public HttpHandler handleAuthenticate(Fun2<String, String, List<String>> getRolesFun) {
		return request -> {
			var attrs = HttpHeaderUtil.getPostedAttrs(request.inputStream);
			var username = attrs.get("username");
			var password = attrs.get("password");
			var roles = getRolesFun.apply(username, password);
			return roles != null ? returnToken(username, roles) : HttpResponse.of(HttpResponse.HTTP403);
		};
	}

	public HttpHandler handleExtendAuth(Fun2<String, String, List<String>> getRolesFun) {
		return verifyToken(List.of(), (username, roles, request) -> returnToken(username, roles));
	}

	public HttpHandler handleFilter(String requiredRole, HttpHandler handler) {
		return verifyToken(List.of(requiredRole), (username, roles, request) -> handler.handle(request));
	}

	private HttpHandler verifyToken( //
			List<String> requiredRoles, //
			FixieFun3<String, List<String>, HttpRequest, HttpResponse> handler1) {
		return request -> {
			var a = request.headers.get("Authentication");
			var sc = new String(aes.decrypt(a), Utf8.charset).split("|");

			return FixieArray.of(sc).map((sig, uer) -> FixieArray.of(uer.split(":")).map((username, exp, rs) -> {
				var expiry = Time.ofYmdHms(exp).epochSec();
				var roles = List.of(rs.split(","));
				var b = Equals.string(sig, sign(uer)) && System.currentTimeMillis() < expiry * 1000l;

				for (var requiredRole : requiredRoles)
					b &= roles.contains(requiredRole);

				return b ? handler1.apply(username, roles, request) : HttpResponse.of(HttpResponse.HTTP403);
			}));
		};
	}

	private HttpResponse returnToken(String username, List<String> roles) {
		var exp = Time.now().addSeconds(timeoutDuration).ymdHms();
		var uer = username + ":" + exp + ":" + Read.from(roles).toJoinedString(",");
		var sig = sign(uer);
		var token = aes.encrypt((sig + "|" + uer).getBytes(Utf8.charset));
		return HttpResponse.of(HttpResponse.HTTP200, token);
	}

	private String sign(String contents) {
		return sha2.sha256Str(rsa.encrypt(contents.getBytes(Utf8.charset)));
	}

}
