package suite.net;

import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import suite.os.Log_;
import suite.os.SocketUtil;
import suite.os.SocketUtil.Io;
import suite.primitive.BooMutable;
import suite.streamlet.Read;
import suite.util.Copy;
import suite.util.Thread_;

public class LoadBalancer {

	private List<String> servers;
	private volatile List<String> alives = new ArrayList<>();
	private AtomicInteger counter = new AtomicInteger();

	private int port = 80;

	public LoadBalancer(List<String> servers) {
		this.servers = servers;
	}

	public void run() {
		var running = BooMutable.true_();

		var probe = new Thread(() -> {
			while (running.isTrue())
				try {
					var alives1 = new ArrayList<String>();

					for (var server : servers)
						try (var socket = new Socket(server, port)) {
							alives1.add(server);
						} catch (SocketException ex) {
						}

					alives = alives1;
					Thread.sleep(500l);
				} catch (Exception ex) {
					Log_.error(ex);
				}
		});

		Io io = (is, os) -> {
			var count = counter.getAndIncrement();
			var alives0 = alives;
			var server = alives0.get(count % alives0.size());

			try (var socket = new Socket(server, port)) {
				var sis = socket.getInputStream();
				var sos = socket.getOutputStream();
				Read.each(Copy.streamByThread(is, sos), Copy.streamByThread(sis, os)).collect(Thread_::startJoin);
			}
		};

		try {
			probe.start();
			new SocketUtil().listenIo(port, io);
		} finally {
			running.setFalse();
		}
	}

}
