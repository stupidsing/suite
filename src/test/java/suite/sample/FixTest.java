package suite.sample;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.junit.Test;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import suite.cfg.Defaults;
import suite.primitive.Chars_;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.adt.map.IntObjMap;
import suite.streamlet.FunUtil.Sink;
import suite.util.String_;
import suite.util.Thread_;

public class FixTest {

	private char sep = 1;

	@Test
	public void testFix() {
		Defaults.bindSecrets("fix .0 .1").map((username, password) -> {
			var fix = formatFix(username, password);

			System.out.println(fix);

			var map = parseFix(fix);

			assertEquals(username, map.get(553));
			return true;
		});
	}

	@Test
	public void testVertx() {
		var portQuote = 5211;
		var portTrade = 5212;
		var port = Boolean.TRUE ? portQuote : portTrade;

		Sink<IntObjMap<String>> handleFix = map -> {
			System.out.println(map);
		};

		Handler<Buffer> handleBuffer = new Handler<Buffer>() {
			private Buffer appender = Buffer.buffer();

			public void handle(Buffer buffer) {
				appender.appendBuffer(buffer);

				var length = buffer.length();
				var p0 = 0;
				var p1 = p0;
				while (p1 < length && appender.getByte(p1++) != sep)
					;
				var p2 = p1;
				while (p2 < length && appender.getByte(p2++) != sep)
					;
				var sizePair = kv(p1, p2);
				var size = sizePair != null ? Integer.valueOf(sizePair[1]) : -1;
				var p3 = 0 <= size ? p2 + size : length;
				var p4 = p3;
				while (p4 < length && appender.getByte(p4++) != sep)
					;
				var checksumPair = kv(p3, p4);
				var checksum = checksumPair != null ? Integer.valueOf(checksumPair[1]) : -1;

				if (0 <= checksum) {
					handleFix.f(parseFix(appender.getString(0, p4)));
					appender = appender.slice(p4, length);
				}
			}

			private String[] kv(int s, int e) {
				return e <= appender.length() ? appender.getString(s, e - 1).split("=") : null;
			}
		};

		var vertx = Vertx.vertx();
		var netClient = vertx.createNetClient();

		try {
			netClient.connect(port, "h4.p.ctrader.cn", ar -> {
				var ns = ar.result();

				ns.upgradeToSsl(void_ -> {
					ns.handler(handleBuffer);
					ns.write(Buffer.buffer(Defaults.bindSecrets("fix .0 .1").map(this::formatFix)));
				});
			});

			Thread_.sleepQuietly(10 * 1000l);
		} finally {
			netClient.close();
			vertx.close();
		}
	}

	private String formatFix(String username, String password) {
		var msgType = "A";
		var senderCompId = "ctrader." + username;
		var targetCompId = "cServer";
		var msgSegNum = 1;
		var sendingTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss", Locale.ENGLISH));
		var targetSubId = "TRADE";
		var senderSubId = "QUOTE"; // TRADE
		var encryptMethod = 0;
		var heartBtInt = 30; // heartbeat seconds
		var resetSeqNumFlag = "Y";

		IntObj_Obj<String, String> f = (k, v) -> k + "=" + v + sep;

		var body = "" //
				+ f.apply(35, msgType) //
				+ f.apply(49, senderCompId) //
				+ f.apply(56, targetCompId) //
				+ f.apply(34, Integer.toString(msgSegNum)) //
				+ f.apply(52, sendingTime) //
				+ f.apply(57, targetSubId) //
				+ f.apply(50, senderSubId) //
				+ f.apply(98, Integer.toString(encryptMethod)) //
				+ f.apply(108, Integer.toString(heartBtInt)) //
				+ f.apply(141, resetSeqNumFlag) //
				+ f.apply(553, username) //
				+ f.apply(554, password);

		var hb = "" //
				+ f.apply(8, "FIX.4.4") //
				+ f.apply(9, Integer.toString(body.length())) //
				+ body;

		var checksum = Chars_.of(hb.toCharArray()).sum() & 0xFF;
		var fix = hb + f.apply(10, String_.right("000" + Integer.toString(checksum), -3));
		return fix;
	}

	private IntObjMap<String> parseFix(String fix) {
		var map = new IntObjMap<String>();
		int p0, p1 = -1;

		while ((p0 = p1 + 1) < fix.length()) {
			p1 = fix.indexOf(sep, p0);
			var pair = String_.split2l(fix.substring(p0, p1), "=");
			map.put(Integer.valueOf(pair.t0), pair.t1);
		}

		return map;
	}

}