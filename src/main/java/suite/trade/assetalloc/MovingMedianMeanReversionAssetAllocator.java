package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.streamlet.Read;
import suite.trade.MovingAverage;
import suite.trade.data.DataSource;

public class MovingMedianMeanReversionAssetAllocator implements AssetAllocator {

	private int windowSize0 = 0;
	private int windowSize1 = 32;

	private MovingAverage ma = new MovingAverage();

	public static AssetAllocator of() {
		return AssetAllocator_.unleverage(new MovingMedianMeanReversionAssetAllocator());
	}

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate) {
		return Read.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					float[] prices = dataSource.prices;
					float[] movingMedian0 = ma.movingMedian(prices, windowSize0);
					float[] movingMedian1 = ma.movingMedian(prices, windowSize1);
					double median0 = movingMedian0[movingMedian0.length - 1];
					double median1 = movingMedian1[movingMedian1.length - 1];
					double diff = median1 - median0;
					return diff / median0;
				}) //
				.toList();
	}

}
