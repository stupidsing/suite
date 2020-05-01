package suite.primitive;

import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.Bytes.BytesBuilder;
import primal.primitive.adt.Bytes.WriteByte;
import primal.puller.Puller;

import static primal.statics.Rethrow.ex;

public class Bytes_ {

	private static int bufferSize = 65536;

	public static Puller<Bytes> buffer(Puller<Bytes> puller) {
		return Puller.of(new BufferedSource(puller) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(byte[] from, int fromIndex, byte[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void copy(Puller<Bytes> puller, WriteByte writer) {
		ex(() -> {
			Bytes bytes;
			while ((bytes = puller.pull()) != null)
				writer.write(bytes.bs, bytes.start, bytes.end - bytes.start);
			return bytes;
		});
	}

	public static Iterate<Puller<Bytes>> split(Bytes delim) {
		var ds = delim.size();

		return puller -> Puller.of(new BufferedSource(puller) {
			protected boolean search() {
				var size = buffer.size();
				while ((p1 = p0 + ds) <= size)
					if (!delim.equals(buffer.range(p0, p1)))
						p0++;
					else
						return true;
				var b = !cont;
				if (b)
					p0 = p1 = buffer.size();
				return b;
			}
		});
	}

	private static abstract class BufferedSource implements Source<Bytes> {
		protected Puller<Bytes> puller;
		protected Bytes buffer = Bytes.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Puller<Bytes> puller) {
			this.puller = puller;
		}

		public Bytes g() {
			Bytes in;
			var bb = new BytesBuilder();
			bb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = puller.pull()) != null)) {
				bb.append(in);
				buffer = bb.toBytes();
			}

			if (cont || 0 < p0) {
				var head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
