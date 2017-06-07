package suite.trade.backalloc;

import suite.streamlet.Streamlet;
import suite.trade.Asset;

public class BackAllocConfiguration {

	public final Streamlet<Asset> assets;
	public final BackAllocator backAllocator;

	public BackAllocConfiguration(Streamlet<Asset> assets, BackAllocator backAllocator) {
		this.assets = assets;
		this.backAllocator = backAllocator;
	}

}
