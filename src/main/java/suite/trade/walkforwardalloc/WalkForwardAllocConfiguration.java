package suite.trade.walkforwardalloc;

import suite.streamlet.Streamlet;
import suite.trade.Asset;

public class WalkForwardAllocConfiguration {

	public final Streamlet<Asset> assets;
	public final WalkForwardAllocator walkForwardAllocator;

	public WalkForwardAllocConfiguration(Streamlet<Asset> assets, WalkForwardAllocator walkForwardAllocator) {
		this.assets = assets;
		this.walkForwardAllocator = walkForwardAllocator;
	}

}
