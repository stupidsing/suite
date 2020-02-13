package suite.primitive;

import static primal.statics.Rethrow.ex;

import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import primal.primitive.adt.Ints;
import primal.primitive.adt.Ints.IntsBuilder;
import primal.primitive.adt.Ints.WriteChar;
import primal.primitive.puller.IntPuller;
import primal.primitive.streamlet.IntStreamlet;
import primal.puller.Puller;

public class Ints_ {

	private static int bufferSize = 65536;

	public static Puller<Ints> buffer(Puller<Ints> puller) {
		return Puller.of(new BufferedSource(puller) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(Puller<Ints> puller, WriteChar writer) {
		ex(() -> {
			Ints ints;
			while ((ints = puller.pull()) != null)
				writer.write(ints.cs, ints.start, ints.end - ints.start);
			return ints;
		});
	}

	public static IntStreamlet reverse(int[] ts, int start, int end) {
		return new IntStreamlet(() -> IntPuller.of(ts, end - 1, start - 1, -1));
	}

	public static Iterate<Puller<Ints>> split(Ints delim) {
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

	private static abstract class BufferedSource implements Source<Ints> {
		protected Puller<Ints> puller;
		protected Ints buffer = Ints.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Puller<Ints> puller) {
			this.puller = puller;
		}

		public Ints g() {
			Ints in;
			var cb = new IntsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = puller.pull()) != null)) {
				cb.append(in);
				buffer = cb.toInts();
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
