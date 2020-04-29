package suite.os;

import static primal.statics.Rethrow.ex;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import primal.Nouns.Buffer;
import primal.Nouns.Utf8;
import primal.Verbs.DeleteFile;
import primal.Verbs.Format;
import primal.Verbs.WriteFile;
import primal.adt.Pair;
import primal.fp.Funs.Source;
import primal.primitive.adt.Bytes;
import primal.puller.Puller;
import primal.streamlet.Streamlet;
import suite.cfg.HomeDir;
import suite.http.HttpClient;
import suite.serialize.SerOutput;

public class StoreCache {

	private ThreadLocal<Boolean> reget = ThreadLocal.withInitial(() -> false);
	private long documentAge;
	private Path dir;

	public class Piper {
		private String sh;

		private Piper(String sh) {
			this.sh = sh;
		}

		public Piper pipe(String command0) {
			var command1 = sh + " | (" + command0 + ")";
			var key = Bytes.of(command1.getBytes(Utf8.charset));

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
		this(HomeDir.dir("store-cache"), 30 * 1000 * 86400l);
	}

	public StoreCache(Path dir, long documentAge) {
		this.dir = dir;
		this.documentAge = documentAge;

		var current = System.currentTimeMillis();
		FileUtil.findPaths(dir).filter(path -> !isUpToDate(path, current)).forEach(DeleteFile::on);
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
		return getPuller(url, HttpClient.get(url)::out);
	}

	public Bytes get(Bytes key, Source<Bytes> source) {
		var puller = getPuller(key, () -> Puller.<Bytes> of(source.g()));
		return puller.collect(Bytes::of);
	}

	public Puller<Bytes> getPuller(String key, Source<Puller<Bytes>> source) {
		var keyBytes = Bytes.of(key.getBytes(Utf8.charset));
		return getPuller(keyBytes, source);
	}

	public Puller<Bytes> getPuller(Bytes key, Source<Puller<Bytes>> source) {
		return ex(() -> {
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

			if (!reget.get() && pair.k) {
				var vis = Files.newInputStream(pair.v);
				var vdis = new DataInputStream(vis);
				return read(vdis).closeAtEnd(vis);
			} else {
				var puller = source.g();
				var vos = WriteFile.to(pair.v);
				var vdo = SerOutput.of(vos);

				return Puller
						.of(() -> ex(() -> {
							var value = puller.pull();
							if (value != null)
								vdo.writeBytes(value);
							return value;
						}))
						.closeAtEnd(vos)
						.closeAtEnd(vdo);
			}
		});
	}

	private Pair<Boolean, Path> match(Bytes key) {
		return ex(() -> {
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
		try (var kos = WriteFile.to(path); SerOutput kdo = SerOutput.of(kos)) {
			kdo.writeInt(key.size());
			kdo.writeBytes(key);
		}
	}

	private Puller<Bytes> read(DataInputStream dis) {
		return Puller.of(new Source<Bytes>() {
			private boolean cont = true;

			public Bytes g() {
				return ex(() -> {
					if (cont) {
						var vb = new byte[Buffer.size];
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
		return current - ex(() -> Files.getLastModifiedTime(path)).toMillis() < documentAge;
	}

	private Path path(Bytes key, int i, String suffix) {
		var hex8 = Format.hex8(key.hashCode());
		var dir1 = dir.resolve(hex8.substring(0, 2));
		return dir1.resolve(hex8 + "." + i + suffix);
	}

}
