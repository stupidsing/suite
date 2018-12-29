package suite.http;

import static suite.util.Friends.fail;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import suite.cfg.Defaults;
import suite.math.Sha2;

public class Jwt {

	private Charset charset = Defaults.charset;
	private Decoder urlDecoder = Base64.getUrlDecoder();
	private Encoder urlEncoder = Base64.getUrlEncoder();

	public String decode(String jwt) {
		var array = jwt.split("\\.");
		var header = decodeUrl(array[0]);
		var payload = decodeUrl(array[1]);
		var hash1 = urlDecoder.decode(array[2]);
		var data = encodeUrl(header) + "." + encodeUrl(payload);
		var hash = new Sha2().sha256(data.getBytes(charset));
		return Arrays.equals(hash, hash1) ? payload : fail();
	}

	public String encode(String payload, String header) {
		var data = encodeUrl(header) + "." + encodeUrl(payload);
		var hash = new Sha2().sha256(data.getBytes(charset));
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
