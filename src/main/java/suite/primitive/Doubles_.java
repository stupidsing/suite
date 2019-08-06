package suite.primitive;

import static primal.statics.Rethrow.ex;

import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import primal.primitive.DblPrim;
import primal.primitive.Int_Dbl;
import primal.primitive.adt.DblMutable;
import primal.primitive.adt.Doubles;
import primal.primitive.adt.Doubles.DoublesBuilder;
import primal.primitive.adt.Doubles.WriteChar;
import primal.primitive.puller.DblPuller;
import primal.puller.Puller;
import suite.primitive.streamlet.DblStreamlet;

public class Doubles_ {

	private static int bufferSize = 65536;

	public static Puller<Doubles> buffer(Puller<Doubles> puller) {
		return Puller.of(new BufferedSource(puller) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(Puller<Doubles> puller, WriteChar writer) {
		ex(() -> {
			Doubles doubles;
			while ((doubles = puller.pull()) != null)
				writer.write(doubles.cs, doubles.start, doubles.end - doubles.start);
			return doubles;
		});
	}

	public static DblStreamlet of(double... ts) {
		return new DblStreamlet(() -> DblPuller.of(ts));
	}

	public static DblStreamlet of(double[] ts, int start, int end, int inc) {
		return new DblStreamlet(() -> DblPuller.of(ts, start, end, inc));
	}

	public static DblStreamlet for_(double s, double e) {
		return new DblStreamlet(() -> {
			var m = DblMutable.of(s);
			return DblPuller.of(() -> {
				var c = m.increment();
				return c < e ? c : DblPrim.EMPTYVALUE;
			});
		});
	}

	public static DblStreamlet reverse(double[] ts, int start, int end) {
		return new DblStreamlet(() -> DblPuller.of(ts, end - 1, start - 1, -1));
	}

	public static Iterate<Puller<Doubles>> split(Doubles delim) {
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

	public static double[] toArray(int length, Int_Dbl f) {
		var cs = new double[length];
		for (var i = 0; i < length; i++)
			cs[i] = f.apply(i);
		return cs;
	}

	private static abstract class BufferedSource implements Source<Doubles> {
		protected Puller<Doubles> puller;
		protected Doubles buffer = Doubles.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Puller<Doubles> puller) {
			this.puller = puller;
		}

		public Doubles g() {
			Doubles in;
			var cb = new DoublesBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = puller.pull()) != null)) {
				cb.append(in);
				buffer = cb.toDoubles();
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
