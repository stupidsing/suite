package suite.trade.walkforwardalloc;

import primal.adt.Pair;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource.AlignKeyDataSource;

import java.util.List;

public interface WalkForwardAllocator {

	public List<Pair<String, Double>> allocate(AlignKeyDataSource<String> akds, int index);

	public default BackAllocator backAllocator() {
		return (akds, indices) -> index -> allocate(akds, index);
	}

}
