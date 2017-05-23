package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.streamlet.Read;
import suite.trade.data.DataSource;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.BuySellStrategy.GetBuySell;
import suite.trade.singlealloc.Strategos;

// old mamr strategy, refer Strategos.movingAvgMeanReverting()
public class MovingAvgAssetAllocator implements AssetAllocator {

	private int nPastDays = 64;
	private int nHoldDays = 8;
	private float threshold = .15f;

	private Strategos strategos = new Strategos();
	private BuySellStrategy mamr = strategos.movingAvgMeanReverting(nPastDays, nHoldDays, threshold);

	public static AssetAllocator of() {
		return AssetAllocator_.unleverage(new MovingAvgAssetAllocator());
	}

	private MovingAvgAssetAllocator() {
	}

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate) {
		return Read.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					float[] prices = dataSource.prices;
					GetBuySell gbs = mamr.analyze(prices);
					int hold = 0;

					for (int i = 0; i < prices.length; i++)
						hold += gbs.get(i);

					return (double) hold;
				}) //
				.toList();
	}

}
