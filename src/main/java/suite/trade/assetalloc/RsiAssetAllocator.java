package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.streamlet.Read;
import suite.trade.data.DataSource;

public class RsiAssetAllocator implements AssetAllocator {

	private int window = 32;
	private double threshold0 = .3f;
	private double threshold1 = .7f;

	public static AssetAllocator of() {
		return AssetAllocator_.unleverage(new RsiAssetAllocator());
	}

	private RsiAssetAllocator() {
	}

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate) {
		return Read.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					float[] prices = dataSource.prices;
					int length = prices.length;
					int u = 0;
					for (int i = length - window; i < length; i++)
						if (prices[i - 1] < prices[i])
							u++;
					float rsi = (float) u / length;
					if (rsi < threshold0)
						return 1d;
					else if (threshold1 < rsi)
						return -1d;
					else
						return 0d;
				}) //
				.toList();
	}

}
