package suite.trade.backalloc;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.backalloc.strategy.BackAllocator_;
import suite.util.FunUtil.Fun;

public class BackAllocConfiguration {

	public final Fun<Time, Streamlet<Asset>> assetsFun;
	public final BackAllocator backAllocator;

	public static BackAllocConfiguration ofSingle(Asset asset) {
		return new BackAllocConfiguration(time -> Read.each(asset), BackAllocator_.ofSingle(asset.symbol));
	}

	public BackAllocConfiguration(Fun<Time, Streamlet<Asset>> assetsFun, BackAllocator backAllocator) {
		this.assetsFun = assetsFun;
		this.backAllocator = backAllocator;
	}

}
