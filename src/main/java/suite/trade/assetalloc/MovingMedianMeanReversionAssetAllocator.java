package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.streamlet.Read;
import suite.trade.MovingAverage;
import suite.trade.data.DataSource;

public class MovingMedianMeanReversionAssetAllocator implements AssetAllocator {

	private int windowSize = 32;

	private MovingAverage ma = new MovingAverage();

	public static AssetAllocator of() {
		return AssetAllocator_.unleverage(new MovingMedianMeanReversionAssetAllocator());
	}

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate) {
		return Read.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					float[] prices = dataSource.prices;
					float[] movingMedian = ma.movingMedian(prices, windowSize);
					double pricex = prices[prices.length - 1];
					double meanx = movingMedian[movingMedian.length - 1];
					return meanx - pricex;
				}) //
				.toList();
	}

}
