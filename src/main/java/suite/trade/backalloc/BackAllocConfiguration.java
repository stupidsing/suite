package suite.trade.backalloc;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;

public class BackAllocConfiguration {

	public final Streamlet<Asset> assets;
	public final BackAllocator backAllocator;

	public static BackAllocConfiguration ofSingle(Asset asset) {
		return new BackAllocConfiguration(Read.each(asset), BackAllocator_.ofSingle(asset.symbol));
	}

	public BackAllocConfiguration(Streamlet<Asset> assets, BackAllocator backAllocator) {
		this.assets = assets;
		this.backAllocator = backAllocator;
	}

}
