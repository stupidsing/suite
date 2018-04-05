package suite.trade.backalloc;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Ints_;
import suite.primitive.Longs_;
import suite.streamlet.Read;
import suite.trade.Time;
import suite.trade.backalloc.BackAllocator.OnDateTime;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;

public class BackAllocatorTest {

	@Test
	public void testStop() {
		Time start = Time.of(2017, 1, 1);
		var symbol = "S";
		float[] prices = { 1f, .99f, .98f, .5f, .5f, .5f, 0f, 0f, 0f, };

		BackAllocator ba0 = (akds, ts) -> index -> List.of(Pair.of(symbol, 1d));
		var ba1 = ba0.stopLoss(.98d);

		var length = prices.length;
		long[] ts = Longs_.toArray(length, i -> start.addDays(i).epochSec());

		DataSource ds = DataSource.of(ts, prices);
		AlignKeyDataSource<String> akds = DataSource.alignAll(Read.from2(List.of(Pair.of(symbol, ds))));
		int[] indices = Ints_.toArray(length, i -> i);

		OnDateTime odt = ba1.allocate(akds, indices);

		var potentials = Ints_ //
				.range(indices.length) //
				.map(index -> 0 < index ? Read.from(odt.onDateTime(index)) : Read.<Pair<String, Double>> empty()) //
				.map(pairs -> pairs.toDouble(Obj_Dbl.sum(pair -> pair.t1))) //
				.toList();

		assertEquals(List.of(0d, 1d, 1d, 1d, 0d, 0d, 0d, 0d, 0d), potentials);
	}

}
