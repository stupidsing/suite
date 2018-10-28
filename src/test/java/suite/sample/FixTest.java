package suite.sample;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.junit.Test;

import suite.primitive.Chars_;
import suite.primitive.adt.map.IntObjMap;
import suite.streamlet.FunUtil.Iterate;
import suite.util.String_;

// https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/samples/sockets/client/SSLSocketClient.java
public class FixTest {

	@Test
	public void test() {
		var username = "3201600";
		var password = "abc123";

		var type = "A";
		var senderCompId = "ctrader." + username;
		var targetCompId = "CSERVER";
		var messageSeq = 1;
		var targetSubId = "TRADE";
		var senderSubId = "any-string";
		var heartbeatSeconds = 30;

		Iterate<String> f = s -> s + "|";

		var body = "" //
				+ f.apply("35=" + type) //
				+ f.apply("49=" + senderCompId) //
				+ f.apply("56=" + targetCompId) //
				+ f.apply("34=" + messageSeq) //
				+ f.apply("52=" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss", Locale.ENGLISH))) //
				+ f.apply("57=" + targetSubId) //
				+ f.apply("50=" + senderSubId) //
				+ f.apply("98=0") //
				+ f.apply("108=" + heartbeatSeconds) //
				+ f.apply("141=Y") //
				+ f.apply("553=" + username) //
				+ f.apply("554=" + password);

		var hb = "" //
				+ f.apply("8=FIX.4.4") //
				+ f.apply("9=" + body.length()) //
				+ body;

		var checksum = Chars_.of(hb.toCharArray()).sum() & 0xFF;

		var fix = hb + f.apply("10=" + checksum);

		System.out.println(fix);

		var map = new IntObjMap<String>();
		int p0, p1 = -1;

		while ((p0 = p1 + 1) < fix.length()) {
			p1 = fix.indexOf("|", p0);
			var pair = String_.split2l(fix.substring(p0, p1), "=");
			map.put(Integer.valueOf(pair.t0), pair.t1);
		}

		assertEquals(username, map.get(553));
	}

}
