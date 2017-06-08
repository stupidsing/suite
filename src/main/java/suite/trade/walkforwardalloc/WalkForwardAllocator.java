package suite.trade.walkforwardalloc;

import java.util.List;

import suite.adt.pair.Pair;
import suite.streamlet.Streamlet2;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;

public interface WalkForwardAllocator {

	public List<Pair<String, Double>> allocate(Streamlet2<String, DataSource> dataSourceBySymbol, int index);

	public default BackAllocator backAllocator() {
		return (dataSourceBySymbol, times) -> (time, index) -> allocate(dataSourceBySymbol, index);
	}

}
