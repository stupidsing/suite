package suite.trade.walkforwardalloc;

import java.util.List;

import suite.adt.pair.Pair;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource.AlignKeyDataSource;

public interface WalkForwardAllocator {

	public List<Pair<String, Double>> allocate(AlignKeyDataSource<String> akds, int index);

	public default BackAllocator backAllocator() {
		return (akds, indices) -> (time, index) -> allocate(akds, index);
	}

}
