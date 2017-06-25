package suite.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import suite.os.LogUtil;
import suite.os.SocketUtil;
import suite.os.SocketUtil.Io;
import suite.primitive.BooMutable;
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

	public void run() throws IOException {
		BooMutable running = BooMutable.true_();

		Thread probe = new Thread(() -> {
			while (running.isTrue())
				try {
					List<String> alives1 = new ArrayList<>();

					for (String server : servers)
						try (Socket socket = new Socket(server, port)) {
							alives1.add(server);
						} catch (SocketException ex) {
						}

					alives = alives1;
					Thread.sleep(500l);
				} catch (Exception ex) {
					LogUtil.error(ex);
				}
		});

		Io io = (is, os) -> {
			int count = counter.getAndIncrement();
			List<String> alives0 = alives;

			String server = alives0.get(count % alives0.size());

			try (Socket socket = new Socket(server, port)) {
				InputStream sis = socket.getInputStream();
				OutputStream sos = socket.getOutputStream();
				List<Thread> threads = Arrays.asList(Copy.streamByThread(is, sos), Copy.streamByThread(sis, os));

				Thread_.startJoin(threads);
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
