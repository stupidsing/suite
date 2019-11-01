import static primal.statics.Rethrow.ex;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

public class MoliuTest {

	@Test
	public void test() throws Exception {
		var b64 = new Object() {
			private String encode(byte[] bs) {
				return Base64.getEncoder().encodeToString(bs);
			}

			private byte[] decode(String s) {
				return Base64.getDecoder().decode(s);
			}
		};

		// RSA asymmetric encryption

		var rsa = new Object() {
			public byte[] e(byte[] bs) throws Exception {
				var kf = KeyFactory.getInstance("RSA");
				var kpg = KeyPairGenerator.getInstance("RSA");

				var keyPair = kpg.generateKeyPair();
				var privateKeyStr = b64.encode(keyPair.getPrivate().getEncoded());
				var publicKeyStr = b64.encode(keyPair.getPublic().getEncoded());

				var enc = Cipher.getInstance("RSA");
				enc.init(Cipher.ENCRYPT_MODE, kf.generatePublic(new X509EncodedKeySpec(b64.decode(publicKeyStr))));
				var encrypted = enc.doFinal(bs);

				var dec = Cipher.getInstance("RSA");
				dec.init(Cipher.DECRYPT_MODE, kf.generatePrivate(new PKCS8EncodedKeySpec(b64.decode(privateKeyStr))));
				return dec.doFinal(encrypted);
			}
		};

		// AES symmetric encryption

		var aes = new Object() {
			private String encrypt(String key, byte[] bs) {
				return b64.encode(encrypt_(key, bs));
			}

			private byte[] decrypt(String key, String s) {
				return decrypt_(b64.decode(s), key);
			}

			private byte[] encrypt_(String key, byte[] s) {
				return ex(() -> {
					var cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
					cipher.init(Cipher.ENCRYPT_MODE, key(key));
					return cipher.doFinal(s);
				});
			}

			private byte[] decrypt_(byte[] s, String key) {
				return ex(() -> {
					var cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
					cipher.init(Cipher.DECRYPT_MODE, key(key));
					return cipher.doFinal(s);
				});
			}

			private SecretKeySpec key(String key0) {
				var sha = ex(() -> MessageDigest.getInstance("SHA-1"));
				var key1 = key0.getBytes(StandardCharsets.UTF_8);
				var key2 = sha.digest(key1);
				var key3 = Arrays.copyOf(key2, 16);
				return new SecretKeySpec(key3, "AES");
			}
		};

		var aesKey = "ssshhhhhhhhhhh!!!!";

		var in = "Secret Message";
		var inbs = in.getBytes(StandardCharsets.UTF_8);

		var rsaOut = rsa.e(inbs);
		var aesOut = aes.decrypt(aesKey, aes.encrypt(aesKey, inbs));

		System.out.println("rsa = " + new String(rsaOut, StandardCharsets.UTF_8));
		System.out.println("aes = " + new String(aesOut, StandardCharsets.UTF_8));
	}

}
