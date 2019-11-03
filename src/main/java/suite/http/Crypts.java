package suite.http;

import static primal.statics.Rethrow.ex;

import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import primal.Nouns.Utf8;
import suite.cfg.HomeDir;

public class Crypts {

	public interface Crypt<E> {
		public E encrypt(byte[] bs);

		public byte[] decrypt(E e);
	}

	// AES symmetric encryption
	public Crypt<String> aes(String aesKey) {
		return cryptStr(aesBs(aesKey));
	}

	// RSA asymmetric encryption
	public Crypt<String> rsa(String ownerSuffix) {
		return cryptStr(rsaBs(ownerSuffix));
	}

	public Crypt<byte[]> aesBs(String aesKey) {
		return ex(() -> {
			var sha = MessageDigest.getInstance("SHA-1");
			var key1 = aesKey.getBytes(Utf8.charset);
			var key2 = sha.digest(key1);
			var key3 = Arrays.copyOf(key2, 16);
			var sks = new SecretKeySpec(key3, "AES");

			var enc = Cipher.getInstance("AES/ECB/PKCS5Padding");
			enc.init(Cipher.ENCRYPT_MODE, sks);

			var dec = Cipher.getInstance("AES/ECB/PKCS5Padding");
			dec.init(Cipher.DECRYPT_MODE, sks);

			return cryptBs(enc, dec);
		});
	}

	public Crypt<byte[]> rsaBs(String ownerSuffix) {
		return ex(() -> {
			var privKeyPath = HomeDir.resolve("private/private.key" + ownerSuffix);
			var pubKeyPath = HomeDir.resolve("private/public.key" + ownerSuffix);

			if (!Files.exists(pubKeyPath)) {
				var kpg = KeyPairGenerator.getInstance("RSA");
				var keyPair = kpg.generateKeyPair();
				Files.writeString(privKeyPath, encode(keyPair.getPrivate().getEncoded()));
				Files.writeString(pubKeyPath, encode(keyPair.getPublic().getEncoded()));
			}

			var kf = KeyFactory.getInstance("RSA");
			var dec = Cipher.getInstance("RSA");
			var enc = Cipher.getInstance("RSA");

			if (Files.exists(privKeyPath)) {
				var privKeyStr = Files.readAllLines(privKeyPath).iterator().next();
				var privKey = new PKCS8EncodedKeySpec(decode(privKeyStr));
				dec.init(Cipher.DECRYPT_MODE, kf.generatePrivate(privKey));
			}

			if (Files.exists(pubKeyPath)) {
				var pubKeyStr = Files.readAllLines(pubKeyPath).iterator().next();
				var pubKey = new X509EncodedKeySpec(decode(pubKeyStr));
				enc.init(Cipher.ENCRYPT_MODE, kf.generatePublic(pubKey));
			}

			return cryptBs(enc, dec);
		});
	}

	private Crypt<String> cryptStr(Crypt<byte[]> crypt) {
		return new Crypt<>() {
			public String encrypt(byte[] bs) {
				return encode(crypt.encrypt(bs));
			}

			public byte[] decrypt(String s) {
				return crypt.decrypt(decode(s));
			}
		};
	}

	private Crypt<byte[]> cryptBs(Cipher enc, Cipher dec) {
		return new Crypt<byte[]>() {
			public byte[] encrypt(byte[] in) {
				return ex(() -> enc.doFinal(in));
			}

			public byte[] decrypt(byte[] in) {
				return ex(() -> dec.doFinal(in));
			}
		};
	}

	private String encode(byte[] bs) {
		return Base64.getEncoder().encodeToString(bs);
	}

	private byte[] decode(String s) {
		return Base64.getDecoder().decode(s);
	}

}
