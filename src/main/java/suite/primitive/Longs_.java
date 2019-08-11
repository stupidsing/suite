package suite.primitive;

import static primal.statics.Rethrow.ex;

import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import primal.primitive.Int_Lng;
import primal.primitive.adt.Longs;
import primal.primitive.adt.Longs.LongsBuilder;
import primal.primitive.adt.Longs.WriteChar;
import primal.primitive.puller.LngPuller;
import primal.primitive.streamlet.LngStreamlet;
import primal.puller.Puller;

public class Longs_ {

	private static int bufferSize = 65536;

	public static Puller<Longs> buffer(Puller<Longs> puller) {
		return Puller.of(new BufferedSource(puller) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(Puller<Longs> puller, WriteChar writer) {
		ex(() -> {
			Longs longs;
			while ((longs = puller.pull()) != null)
				writer.write(longs.cs, longs.start, longs.end - longs.start);
			return longs;
		});
	}

	public static LngStreamlet reverse(long[] ts, int start, int end) {
		return new LngStreamlet(() -> LngPuller.of(ts, end - 1, start - 1, -1));
	}

	public static Iterate<Puller<Longs>> split(Longs delim) {
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

	public static long[] toArray(int length, Int_Lng f) {
		var cs = new long[length];
		for (var i = 0; i < length; i++)
			cs[i] = f.apply(i);
		return cs;
	}

	private static abstract class BufferedSource implements Source<Longs> {
		protected Puller<Longs> puller;
		protected Longs buffer = Longs.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Puller<Longs> puller) {
			this.puller = puller;
		}

		public Longs g() {
			Longs in;
			var cb = new LongsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = puller.pull()) != null)) {
				cb.append(in);
				buffer = cb.toLongs();
			}

			if (cont && 0 < p0) {
				var head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
