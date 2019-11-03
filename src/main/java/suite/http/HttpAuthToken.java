package suite.http;

import static primal.statics.Rethrow.ex;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import primal.MoreVerbs.Pull;
import primal.MoreVerbs.Read;
import primal.Nouns.Utf8;
import primal.Verbs.Equals;
import primal.Verbs.ReadString;
import primal.adt.FixieArray;
import primal.adt.Fixie_.FixieFun3;
import primal.fp.Funs2.Fun2;
import primal.primitive.adt.Bytes;
import primal.puller.Puller;
import suite.cfg.Defaults;
import suite.http.Crypts.Crypt;
import suite.http.Http.Handler;
import suite.http.Http.Request;
import suite.http.Http.Response;
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
	private Crypt<byte[]> rsa = crypts.rsaBs(Defaults.salt);
	private Sha2 sha2 = new Sha2();

	private ObjectMapper om = new ObjectMapper();

	public Handler handleLogin(String authenticatePath) {
		return request -> Response.of(Pull.from("<html>" //
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

	public Handler handleGetToken(Fun2<String, String, List<String>> getRolesFun) {
		return request -> {
			var jsonStr = ReadString.from(request.inputStream);
			var json = ex(() -> om.readTree(jsonStr));
			var username = json.path("username").asText();
			var password = json.path("password").asText();
			var roles = getRolesFun.apply(username, password);
			return roles != null ? returnToken(username, roles) : Response.of(Http.S403);
		};
	}

	public Handler handleExtendToken(Fun2<String, String, List<String>> getRolesFun) {
		return verifyToken(List.of(), (username, roles, request) -> returnToken(username, roles));
	}

	public Handler handleFilter(String requiredRole, Handler handler) {
		return verifyToken(List.of(requiredRole), (username, roles, request) -> handler.handle(request));
	}

	private Handler verifyToken( //
			List<String> requiredRoles, //
			FixieFun3<String, List<String>, Request, Response> handler1) {
		return request -> {
			var a = request.headers.get("Authentication");
			var sc = new String(aes.decrypt(a), Utf8.charset).split("|");

			return FixieArray.of(sc).map((sig, uer) -> FixieArray.of(uer.split(":")).map((username, exp, rs) -> {
				var expiry = Time.ofYmdHms(exp).epochSec();
				var roles = List.of(rs.split(","));
				var b = Equals.string(sig, sign(uer)) && System.currentTimeMillis() < expiry * 1000l;

				for (var requiredRole : requiredRoles)
					b &= roles.contains(requiredRole);

				return b ? handler1.apply(username, roles, request) : Response.of(Http.S403);
			}));
		};
	}

	private Response returnToken(String username, List<String> roles) {
		var exp = Time.now().addSeconds(timeoutDuration).ymdHms();
		var uer = username + ":" + exp + ":" + Read.from(roles).toJoinedString(",");
		var sig = sign(uer);
		var token = aes.encrypt((sig + "|" + uer).getBytes(Utf8.charset));
		var node = om.createObjectNode().put("token", token);
		var bs = ex(() -> om.writeValueAsBytes(node));
		return Response.of(Puller.<Bytes> of(Bytes.of(bs)));
	}

	private String sign(String contents) {
		return sha2.sha256Str(rsa.encrypt(contents.getBytes(Utf8.charset)));
	}

}