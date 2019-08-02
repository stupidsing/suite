package suite.http;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import com.fasterxml.jackson.databind.ObjectMapper;

import primal.Nouns.Utf8;
import primal.Verbs.Equals;
import suite.math.Sha2;

public class Jwt {

	private Charset charset = Utf8.charset;
	private ObjectMapper om = new ObjectMapper();
	private Decoder urlDecoder = Base64.getUrlDecoder();
	private Encoder urlEncoder = Base64.getUrlEncoder();
	private Sha2 sha2 = new Sha2();

	private String header = "{ \"typ\": \"JWT\", \"alg\": \"HS256\" }";

	public String decode(String jwt) {
		var array = jwt.split("\\.");
		var header = decodeUrl(array[0]);
		var payload = decodeUrl(array[1]);
		var hash1 = urlDecoder.decode(array[2]);

		var json = ex(() -> om.readTree(header));
		var b = Equals.string(json.path("typ").textValue(), "JWT") && Equals.string(json.path("alg").textValue(), "HS256");
		var data = encodeUrl(header) + "." + encodeUrl(payload);
		var hash = sha2.sha256(data.getBytes(charset)); // sha2.hmac(secret, data.getBytes(charset))
		return b && Arrays.equals(hash, hash1) ? payload : fail();
	}

	public String encode(String payload) {
		var data = encodeUrl(header) + "." + encodeUrl(payload);
		var hash = sha2.sha256(data.getBytes(charset));
		var sig = urlEncoder.encodeToString(hash);
		var jwt = data + "." + sig;
		return jwt;
	}

	private String decodeUrl(String in) {
		return new String(urlDecoder.decode(in), charset);
	}

	private String encodeUrl(String in) {
		return urlEncoder.encodeToString(in.getBytes(charset));
	}

}
