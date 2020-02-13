package suite.primitive;

import static primal.statics.Rethrow.ex;

import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import primal.primitive.adt.Floats;
import primal.primitive.adt.Floats.FloatsBuilder;
import primal.primitive.adt.Floats.WriteChar;
import primal.primitive.puller.FltPuller;
import primal.primitive.streamlet.FltStreamlet;
import primal.puller.Puller;

public class Floats_ {

	private static int bufferSize = 65536;

	public static Puller<Floats> buffer(Puller<Floats> puller) {
		return Puller.of(new BufferedSource(puller) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(Puller<Floats> puller, WriteChar writer) {
		ex(() -> {
			Floats floats;
			while ((floats = puller.pull()) != null)
				writer.write(floats.cs, floats.start, floats.end - floats.start);
			return floats;
		});
	}

	public static FltStreamlet reverse(float[] ts, int start, int end) {
		return new FltStreamlet(() -> FltPuller.of(ts, end - 1, start - 1, -1));
	}

	public static Iterate<Puller<Floats>> split(Floats delim) {
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

	private static abstract class BufferedSource implements Source<Floats> {
		protected Puller<Floats> puller;
		protected Floats buffer = Floats.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Puller<Floats> puller) {
			this.puller = puller;
		}

		public Floats g() {
			Floats in;
			var cb = new FloatsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = puller.pull()) != null)) {
				cb.append(in);
				buffer = cb.toFloats();
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
