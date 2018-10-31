package suite.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import suite.cfg.Defaults;
import suite.os.Log_;
import suite.primitive.Chars_;
import suite.primitive.adt.map.IntObjMap;
import suite.streamlet.FunUtil.Sink;
import suite.util.String_;

public class FixTest {

	private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss", Locale.ENGLISH);
	private char sep = 1;

	@Test
	public void testFix() {
		Defaults.bindSecrets("fix .0 .1").map((username, password) -> {
			var fix = new FormatFix(username).logon(password);
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

		Sink<IntObjMap<String>> handleFix = map -> Log_.info(map.toString());

		Handler<Buffer> handleBuffer = new Handler<Buffer>() {
			private Buffer appender = Buffer.buffer();

			public void handle(Buffer buffer) {
				if (Boolean.TRUE)
					Log_.info("Received " + buffer.toString());

				appender.appendBuffer(buffer);

				var length = buffer.length();
				var p0 = 0;
				var p1 = p0;
				int p2, p3;
				boolean b;

				b = false;
				while ((p2 = p1 + 4) <= length && !(b = String_.equals(appender.getString(p1, p1 + 4), sep + "10=")))
					p1++;

				b = false;
				while ((p3 = p2 + 1) <= length && !(b = appender.getByte(p2) == sep))
					p2++;

				if (b) {
					handleFix.f(parseFix(appender.getString(0, p3)));
					appender = appender.slice(p3, length);
				}
			}
		};

		var vertx = Vertx.vertx();
		var netClient = vertx.createNetClient();

		try {
			var isEnded = new CompletableFuture<Boolean>();

			netClient.connect(port, "h4.p.ctrader.cn", ar -> {
				var ns = ar.result();

				ns.upgradeToSsl(void0 -> {
					ns.closeHandler(void1 -> {
						Log_.error(new RuntimeException("closed"));
						isEnded.complete(false);
					});

					ns.handler(handleBuffer);

					Sink<String> write = s -> {
						ns.write(s);
						System.out.println("wrote " + s);
					};

					Defaults.bindSecrets("fix .0 .1").map((username, password) -> {
						var ff = new FormatFix(username);

						vertx.setTimer(500l, t0 -> {
							write.f(ff.logon(password));
							vertx.setTimer(500l, t1 -> {
								write.f(ff.heartbeat());
								vertx.setTimer(500l, t2 -> {
									write.f(ff.testRequest());
									vertx.setTimer(500l, t3 -> {
										write.f(ff.marketDataRequest());
										vertx.setTimer(500l, t4 -> {
											write.f(ff.logout());
											vertx.setTimer(500l, t5 -> {
												isEnded.complete(true);
											});
										});
									});
								});
							});
						});

						// return ns.write("" //
						// + ff.logon(password) //
						// + ff.heartbeat() //
						// + ff.testRequest() //
						// + ff.marketDataRequest() //
						// + ff.logout());

						return true;
					});
				});
			});

			assertTrue(isEnded.join());
		} finally {
			netClient.close();
			vertx.close();
		}
	}

	private class FormatFix {
		private String username;
		private int msgSegNum = 1;

		private FormatFix(String username) {
			this.username = username;
		}

		private String heartbeat() {
			return format("5", "");
		}

		private String logout() {
			return format("5", "");
		}

		private String logon(String password) {
			var encryptMethod = 0;
			var heartBtInt = 30; // heartbeat seconds
			var resetSeqNumFlag = "Y";

			return format("A", "" //
					+ f(98, Integer.toString(encryptMethod)) //
					+ f(108, Integer.toString(heartBtInt)) //
					+ f(141, resetSeqNumFlag) //
					+ f(553, username) //
					+ f(554, password));
		}

		private String marketDataRequest() {
			return format("A", "" //
					+ f(262, "EURUSD:WDqsoT") //
					+ f(263, "1") //
					+ f(264, "0") //
					+ f(265, "1") //
					+ f(267, "2") //
					+ f(269, "0") //
					+ f(269, "1") //
					+ f(46, "1") //
					+ f(55, "1"));
		}

		private String testRequest() {
			return format("1", f(112, "test you"));
		}

		private String format(String msgType, String m0) {
			var senderCompId = "ctrader." + username;
			var targetCompId = "cServer";
			var sendingTime = Instant.now().atOffset(ZoneOffset.UTC).format(dtf);
			var targetSubId = "QUOTE"; // TRADE
			var senderSubId = "S" + UUID.randomUUID().toString().substring(0, 7);

			var m1 = "" //
					+ f(35, msgType) //
					+ f(49, senderCompId) //
					+ f(56, targetCompId) //
					+ f(34, Integer.toString(msgSegNum++)) //
					+ f(52, sendingTime) //
					+ f(57, targetSubId) //
					+ f(50, senderSubId) //
					+ m0;

			var m2 = "" //
					+ f(8, "FIX.4.4") //
					+ f(9, Integer.toString(m1.length())) //
					+ m1;

			var checksum = Chars_.of(m2.toCharArray()).sum() & 0xFF;
			return m2 + f(10, String_.right("000" + Integer.toString(checksum), -3));
		}

		private String f(int k, String v) {
			return v != null ? k + "=" + v + sep : "";
		}
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