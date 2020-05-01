package suite.http;

import primal.Nouns.Utf8;
import primal.os.Log_;
import suite.cfg.HomeDir;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Callable;

import static primal.statics.Rethrow.ex;

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

	public Crypt<byte[]> aesBs(String key0) {
		return ex(() -> {
			var sha = MessageDigest.getInstance("SHA-1");
			var key1 = key0.getBytes(Utf8.charset);
			var key2 = sha.digest(key1);
			var key3 = Arrays.copyOf(key2, 16);
			var sks = new SecretKeySpec(key3, "AES");

			return cryptBs(() -> {
				var enc = Cipher.getInstance("AES/ECB/PKCS5Padding");
				enc.init(Cipher.ENCRYPT_MODE, sks);
				return enc;
			}, () -> {
				var dec = Cipher.getInstance("AES/ECB/PKCS5Padding");
				dec.init(Cipher.DECRYPT_MODE, sks);
				return dec;
			});
		});
	}

	public Crypt<byte[]> rsaBs(String ownerSuffix) {
		return ex(() -> {
			var pubKeyPath = HomeDir.priv("public.key" + ownerSuffix);
			var privKeyPath = HomeDir.priv("private.key" + ownerSuffix);

			if (!Files.exists(pubKeyPath)) {
				Log_.info("generating key for owner " + ownerSuffix);
				var kpg = KeyPairGenerator.getInstance("RSA");
				var keyPair = kpg.generateKeyPair();
				Files.writeString(pubKeyPath, encode(keyPair.getPublic().getEncoded()));
				Files.writeString(privKeyPath, encode(keyPair.getPrivate().getEncoded()));
			}

			var kf = KeyFactory.getInstance("RSA");
			PublicKey pubKey;
			PrivateKey privKey;

			if (Files.exists(pubKeyPath)) {
				var pubKeyStr = Files.readAllLines(pubKeyPath).iterator().next();
				pubKey = kf.generatePublic(new X509EncodedKeySpec(decode(pubKeyStr)));
			} else
				pubKey = null;

			if (Files.exists(privKeyPath)) {
				var privKeyStr = Files.readAllLines(privKeyPath).iterator().next();
				privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(decode(privKeyStr)));
			} else
				privKey = null;

			return cryptBs(() -> {
				var enc = Cipher.getInstance("RSA");
				enc.init(Cipher.ENCRYPT_MODE, pubKey);
				return enc;
			}, () -> {
				var dec = Cipher.getInstance("RSA");
				dec.init(Cipher.DECRYPT_MODE, privKey);
				return dec;
			});
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

	private Crypt<byte[]> cryptBs(Callable<Cipher> enc, Callable<Cipher> dec) {
		return new Crypt<byte[]>() {
			public byte[] encrypt(byte[] in) {
				return ex(() -> enc.call().doFinal(in));
			}

			public byte[] decrypt(byte[] in) {
				return ex(() -> dec.call().doFinal(in));
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
