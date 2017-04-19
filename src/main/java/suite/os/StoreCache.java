package suite.os;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import suite.Constants;
import suite.http.HttpUtil;
import suite.primitive.Bytes;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Source;
import suite.util.HomeDir;
import suite.util.Rethrow;
import suite.util.To;

public class StoreCache {

	private Path dir = HomeDir.resolve("store-cache");

	public Outlet<Bytes> http(String urlString) {
		URL url = Rethrow.ex(() -> new URL(urlString));
		return getOutlet(urlString, () -> HttpUtil.http("GET", url).out);
	}

	public Bytes get(Bytes key, Source<Bytes> source) {
		Outlet<Bytes> outlet = getOutlet(key, () -> Outlet.<Bytes> of(source.source()));
		return outlet.collect(As::bytes);
	}

	public Outlet<Bytes> getOutlet(String key, Source<Outlet<Bytes>> source) {
		Bytes keyBytes = Bytes.of(key.getBytes(Constants.charset));
		return getOutlet(keyBytes, source);
	}

	@SuppressWarnings("resource")
	public Outlet<Bytes> getOutlet(Bytes key, Source<Outlet<Bytes>> source) {
		return Rethrow.ex(() -> {
			int keySize = key.size();
			String hex8 = To.hex8(key.hashCode());
			Path dir1 = dir.resolve(hex8.substring(0, 2));
			int i = 0;
			Path path;

			while (Files.exists(path = dir1.resolve(hex8 + "." + i))) {
				InputStream is = Files.newInputStream(path);
				DataInputStream dis = new DataInputStream(is);

				if (dis.readInt() == keySize) {
					byte kb[] = new byte[keySize];
					dis.readFully(kb);
					if (Arrays.equals(key.toByteArray(), kb))
						return Outlet.of(new Source<Bytes>() {
							private boolean cont = true;

							public Bytes source() {
								return Rethrow.ex(() -> {
									if (cont) {
										byte vb[] = new byte[Constants.bufferSize];
										int n, nBytesRead = 0;
										while (nBytesRead < vb.length
												&& (cont &= 0 <= (n = dis.read(vb, nBytesRead, vb.length - nBytesRead))))
											nBytesRead += n;
										return Bytes.of(vb, 0, nBytesRead);
									} else {
										dis.close();
										is.close();
										return null;
									}
								});
							}
						});
				}

				dis.close();
				is.close();
				i++;
			}

			Outlet<Bytes> outlet = source.source();
			OutputStream os = FileUtil.out(path);
			DataOutputStream dos = new DataOutputStream(os);
			DataOutput do_ = dos;

			do_.writeInt(keySize);
			do_.write(key.toByteArray());

			return Outlet //
					.of(() -> Rethrow.ex(() -> {
						Bytes value = outlet.next();
						if (value != null)
							value.write(do_);
						return value;
					})) //
					.closeAtEnd(dos) //
					.closeAtEnd(os);
		});
	}

}
