package suite.trade.backalloc;

import org.junit.jupiter.api.Test;
import primal.MoreVerbs.Read;
import primal.adt.Pair;
import primal.primitive.fp.AsDbl;
import primal.primitive.fp.AsInt;
import primal.primitive.fp.AsLng;
import suite.trade.Time;
import suite.trade.data.DataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static suite.util.Streamlet_.forInt;

public class BackAllocatorTest {

	@Test
	public void testStop() {
		var start = Time.of(2017, 1, 1);
		var symbol = "S";
		float[] prices = { 1f, .99f, .98f, .5f, .5f, .5f, 0f, 0f, 0f, };

		BackAllocator ba0 = (akds, ts) -> index -> List.of(Pair.of(symbol, 1d));
		var ba1 = ba0.stopLoss(.98d);

		var length = prices.length;
		var ts = AsLng.array(length, i -> start.addDays(i).epochSec());

		var ds = DataSource.of(ts, prices);
		var akds = DataSource.alignAll(Read.from2(List.of(Pair.of(symbol, ds))));
		var indices = AsInt.array(length, i -> i);

		var odt = ba1.allocate(akds, indices);

		var potentials = forInt(indices.length) //
				.map(index -> 0 < index ? Read.from(odt.onDateTime(index)) : Read.<Pair<String, Double>> empty()) //
				.map(pairs -> pairs.toDouble(AsDbl.sum(pair -> pair.v))) //
				.toList();

		assertEquals(List.of(0d, 1d, 1d, 1d, 0d, 0d, 0d, 0d, 0d), potentials);
	}

}
