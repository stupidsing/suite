package suite.os;

import static suite.util.Friends.rethrow;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import suite.adt.pair.Pair;
import suite.cfg.Defaults;
import suite.cfg.HomeDir;
import suite.http.HttpUtil;
import suite.primitive.Bytes;
import suite.serialize.SerOutput;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Puller;
import suite.streamlet.Streamlet;
import suite.util.To;

public class StoreCache {

	private ThreadLocal<Boolean> reget = ThreadLocal.withInitial(() -> false);
	private int documentAge = 30;
	private Path dir;

	public class Piper {
		private String sh;

		private Piper(String sh) {
			this.sh = sh;
		}

		public Piper pipe(String command0) {
			var command1 = sh + " | (" + command0 + ")";
			var key = Bytes.of(command1.getBytes(Defaults.charset));

			return match(key).map((isCached, file) -> {
				if (!isCached)
					Execute.shell(command1 + " > '" + file + "'");
				return new Piper("cat '" + file + "'");
				// return new Pipe("" + command1 + " | tee '" + file + "'");
			});
		}

		public Streamlet<String> read() {
			return Pipe.shell(sh);
		}
	}

	public StoreCache() {
		this(HomeDir.dir("store-cache"));
	}

	public StoreCache(Path dir) {
		this.dir = dir;

		var current = System.currentTimeMillis();
		FileUtil.findPaths(dir).filter(path -> !isUpToDate(path, current)).forEach(FileUtil::delete);
	}

	public <T> T reget(Source<T> source) {
		var reget0 = reget.get();
		try {
			reget.set(true);
			return source.g();
		} finally {
			reget.set(reget0);
		}
	}

	public Piper pipe(String in) {
		return new Piper("echo '" + in + "'");
	}

	public Puller<Bytes> http(String url) {
		return getPuller(url, HttpUtil.get(url)::out);
	}

	public Bytes get(Bytes key, Source<Bytes> source) {
		var puller = getPuller(key, () -> Puller.<Bytes> of(source.g()));
		return puller.collect(Bytes::of);
	}

	public Puller<Bytes> getPuller(String key, Source<Puller<Bytes>> source) {
		var keyBytes = Bytes.of(key.getBytes(Defaults.charset));
		return getPuller(keyBytes, source);
	}

	public Puller<Bytes> getPuller(Bytes key, Source<Puller<Bytes>> source) {
		return rethrow(() -> {
			var current = System.currentTimeMillis();
			Path path;
			var i = 0;

			while (Files.exists(path = path(key, i++, "")))
				if (isUpToDate(path, current)) {
					var is = Files.newInputStream(path);
					var dis = new DataInputStream(is);
					if (isMatch(key, dis))
						return read(dis).closeAtEnd(is);
					dis.close();
					is.close();
				} else {
					Files.delete(path);
					break;
				}

			var pair = match(key);

			if (!reget.get() && pair.t0) {
				var vis = Files.newInputStream(pair.t1);
				var vdis = new DataInputStream(vis);
				return read(vdis).closeAtEnd(vis);
			} else {
				var puller = source.g();
				var vos = FileUtil.out(pair.t1);
				var vdo = SerOutput.of(vos);

				return Puller //
						.of(() -> rethrow(() -> {
							var value = puller.pull();
							if (value != null)
								vdo.writeBytes(value);
							return value;
						})) //
						.closeAtEnd(vos) //
						.closeAtEnd(vdo);
			}
		});
	}

	private Pair<Boolean, Path> match(Bytes key) {
		return rethrow(() -> {
			var current = System.currentTimeMillis();
			var i = 0;
			Path path;

			while (Files.exists(path = path(key, i, ".k"))) {
				if (isUpToDate(path, current))
					try (var kis = Files.newInputStream(path); var kdis = new DataInputStream(kis)) {
						if (isMatch(key, kdis) && Files.exists(path = path(key, i, ".v")))
							return Pair.of(true, path);
					}
				else {
					Files.delete(path);
					break;
				}
				i++;
			}

			writeKey(path, key);
			return Pair.of(false, path(key, i, ".v"));
		});
	}

	private boolean isMatch(Bytes key, DataInputStream dis) throws IOException {
		var keySize = key.size();

		if (dis.readInt() == keySize) {
			var kb = new byte[keySize];
			dis.readFully(kb);
			return Arrays.equals(key.toArray(), kb);
		} else
			return false;
	}

	private void writeKey(Path path, Bytes key) throws IOException {
		try (var kos = FileUtil.out(path); SerOutput kdo = SerOutput.of(kos)) {
			kdo.writeInt(key.size());
			kdo.writeBytes(key);
		}
	}

	private Puller<Bytes> read(DataInputStream dis) {
		return Puller.of(new Source<Bytes>() {
			private boolean cont = true;

			public Bytes g() {
				return rethrow(() -> {
					if (cont) {
						var vb = new byte[Defaults.bufferSize];
						int n, nBytesRead = 0;
						while (nBytesRead < vb.length && (cont &= 0 <= (n = dis.read(vb, nBytesRead, vb.length - nBytesRead))))
							nBytesRead += n;
						return Bytes.of(vb, 0, nBytesRead);
					} else {
						dis.close();
						return null;
					}
				});
			}
		});
	}

	private boolean isUpToDate(Path path, long current) {
		return current - rethrow(() -> Files.getLastModifiedTime(path)).toMillis() < 1000l * 86400 * documentAge;
	}

	private Path path(Bytes key, int i, String suffix) {
		var hex8 = To.hex8(key.hashCode());
		var dir1 = dir.resolve(hex8.substring(0, 2));
		return dir1.resolve(hex8 + "." + i + suffix);
	}

}
