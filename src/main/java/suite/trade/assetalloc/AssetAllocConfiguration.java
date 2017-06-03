package suite.trade.assetalloc;

import suite.streamlet.Streamlet;
import suite.trade.Asset;

public class AssetAllocConfiguration {

	public final Streamlet<Asset> assets;
	public final AssetAllocator assetAllocator;

	public AssetAllocConfiguration(Streamlet<Asset> assets, AssetAllocator assetAllocator) {
		this.assets = assets;
		this.assetAllocator = assetAllocator;
	}

}
