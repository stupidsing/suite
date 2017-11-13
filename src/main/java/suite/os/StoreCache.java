package suite.os;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import suite.Constants;
import suite.adt.pair.Pair;
import suite.http.HttpUtil;
import suite.primitive.Bytes;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.DataOutput_;
import suite.util.FunUtil.Source;
import suite.util.HomeDir;
import suite.util.Rethrow;
import suite.util.To;

public class StoreCache {

	private int documentAge = 30;
	private Path dir = HomeDir.resolve("store-cache");

	public StoreCache() {
		long current = System.currentTimeMillis();

		LogUtil.info(FileUtil.findPaths(dir) //
				.filter(path -> !isUpToDate(path, current)) //
				.map(path -> "\nrm '" + path + "'") //
				.collect(As::joined));
	}

	public String sh(String command) {
		Bytes key = Bytes.of(command.getBytes(Constants.charset));
		Pair<Boolean, Path> pair = match(key);

		if (pair.t0)
			return "cat '" + pair.t1 + "'";
		else
			return "(" + command + ") | tee '" + pair.t1 + "'";
	}

	public Outlet<Bytes> http(String urlString) {
		URL url = To.url(urlString);
		return getOutlet(urlString, () -> HttpUtil.get(url).out);
	}

	public Bytes get(Bytes key, Source<Bytes> source) {
		Outlet<Bytes> outlet = getOutlet(key, () -> Outlet.<Bytes> of(source.source()));
		return outlet.collect(Bytes::of);
	}

	public Outlet<Bytes> getOutlet(String key, Source<Outlet<Bytes>> source) {
		Bytes keyBytes = Bytes.of(key.getBytes(Constants.charset));
		return getOutlet(keyBytes, source);
	}

	public Outlet<Bytes> getOutlet(Bytes key, Source<Outlet<Bytes>> source) {
		return Rethrow.ex(() -> {
			long current = System.currentTimeMillis();
			Path path;
			int i = 0;

			while (Files.exists(path = path(key, i++, "")))
				if (isUpToDate(path, current)) {
					InputStream is = Files.newInputStream(path);
					DataInputStream dis = new DataInputStream(is);
					if (isMatch(key, dis))
						return read(dis).closeAtEnd(is);
					dis.close();
					is.close();
				} else {
					Files.delete(path);
					break;
				}

			Pair<Boolean, Path> pair = match(key);

			if (pair.t0) {
				InputStream vis = Files.newInputStream(pair.t1);
				DataInputStream vdis = new DataInputStream(vis);
				return read(vdis).closeAtEnd(vis);
			} else {
				Outlet<Bytes> outlet = source.source();
				OutputStream vos = FileUtil.out(pair.t1);
				DataOutput_ vdo = DataOutput_.of(vos);

				return Outlet //
						.of(() -> Rethrow.ex(() -> {
							Bytes value = outlet.next();
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
		return Rethrow.ex(() -> {
			long current = System.currentTimeMillis();
			int i = 0;
			Path path;

			while (Files.exists(path = path(key, i, ".k"))) {
				if (isUpToDate(path, current))
					try (InputStream kis = Files.newInputStream(path); DataInputStream kdis = new DataInputStream(kis)) {
						if (isMatch(key, kdis))
							return Pair.of(false, path(key, i, ".v"));
					}
				else {
					Files.delete(path);
					break;
				}
				i++;
			}

			writeKey(path, key);
			return Pair.of(true, path(key, i, ".v"));
		});
	}

	private boolean isMatch(Bytes key, DataInputStream dis) throws IOException {
		int keySize = key.size();

		if (dis.readInt() == keySize) {
			byte[] kb = new byte[keySize];
			dis.readFully(kb);
			return Arrays.equals(key.toArray(), kb);
		} else
			return false;
	}

	private void writeKey(Path path, Bytes key) throws IOException {
		try (OutputStream kos = FileUtil.out(path); DataOutput_ kdo = DataOutput_.of(kos)) {
			kdo.writeInt(key.size());
			kdo.writeBytes(key);
		}
	}

	private Outlet<Bytes> read(DataInputStream dis) {
		return Outlet.of(new Source<Bytes>() {
			private boolean cont = true;

			public Bytes source() {
				return Rethrow.ex(() -> {
					if (cont) {
						byte[] vb = new byte[Constants.bufferSize];
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
		return current - Rethrow.ex(() -> Files.getLastModifiedTime(path)).toMillis() < 1000l * 86400 * documentAge;
	}

	private Path path(Bytes key, int i, String suffix) {
		String hex8 = To.hex8(key.hashCode());
		Path dir1 = dir.resolve(hex8.substring(0, 2));
		return dir1.resolve(hex8 + "." + i + suffix);
	}

}
