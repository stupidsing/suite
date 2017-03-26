package suite.os;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import suite.primitive.Bytes;
import suite.util.FunUtil.Source;
import suite.util.Rethrow;
import suite.util.TempDir;
import suite.util.To;

public class StoreCache {

	private static Path dir = TempDir.resolve("/");

	public Bytes get(Bytes key, Source<Bytes> source) {
		return Rethrow.ioException(() -> {
			int keySize = key.size();
			String hex8 = To.hex8(key.hashCode());
			Path dir1 = dir.resolve(hex8.substring(0, 2));
			int i = 0;
			Path path;

			while (Files.exists(path = dir1.resolve(hex8 + "." + i))) {
				try (InputStream is = Files.newInputStream(path); DataInputStream dis = new DataInputStream(is)) {
					if (dis.readInt() == keySize) {
						byte kb[] = new byte[keySize];
						dis.readFully(kb);
						if (Arrays.equals(key.toBytes(), kb)) {
							int valueSize = dis.readInt();
							byte vb[] = new byte[valueSize];
							dis.readFully(vb);
							return Bytes.of(vb);
						}
					}
				}

				i++;
			}

			Bytes value = source.source();

			try (OutputStream os = FileUtil.out(path); DataOutputStream dos = new DataOutputStream(os)) {
				dos.write(keySize);
				dos.write(key.toBytes());
				dos.write(value.size());
				dos.write(value.toBytes());
				return value;
			}
		});
	}

}
