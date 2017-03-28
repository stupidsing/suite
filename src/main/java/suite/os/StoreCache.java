package suite.os;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import suite.Constants;
import suite.primitive.Bytes;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Source;
import suite.util.Rethrow;
import suite.util.TempDir;
import suite.util.To;

public class StoreCache {

	private static Path dir = TempDir.resolve("store-cache");

	public Bytes get(Bytes key, Source<Bytes> source) {
		Outlet<Bytes> outlet = getOutlet(key, () -> new Outlet<>(source));
		return outlet.collect(As::bytes);
	}

	@SuppressWarnings("resource")
	public Outlet<Bytes> getOutlet(Bytes key, Source<Outlet<Bytes>> source) {
		return Rethrow.ioException(() -> {
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
					if (Arrays.equals(key.toBytes(), kb))
						return new Outlet<>(() -> Rethrow.ioException(() -> {
							byte vb[] = new byte[Constants.bufferSize];
							int n, nBytesRead = 0;
							while (nBytesRead < vb.length)
								if (0 <= (n = dis.read(vb, nBytesRead, vb.length - nBytesRead)))
									nBytesRead += n;
								else {
									dis.close();
									is.close();
									break;
								}
							return Bytes.of(vb);
						}));
				}

				i++;
			}

			Outlet<Bytes> outlet = source.source();
			OutputStream os = FileUtil.out(path);
			DataOutputStream dos = new DataOutputStream(os);

			dos.writeInt(keySize);
			dos.write(key.toBytes());

			return new Outlet<>(() -> Rethrow.ioException(() -> {
				Bytes value = outlet.next();
				if (value != null)
					dos.write(value.toBytes());
				else {
					dos.close();
					os.close();
				}
				return value;
			}));
		});
	}

}
