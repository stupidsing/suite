package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.streamlet.Read;
import suite.trade.data.DataSource;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.BuySellStrategy.GetBuySell;
import suite.trade.singlealloc.Strategos;
import suite.util.To;

// old mamr strategy, refer Strategos.movingAvgMeanReverting()
public class MovingAvgAssetAllocator implements AssetAllocator {

	private int nPastDays = 64;
	private int nHoldDays = 8;
	private float threshold = .15f;

	private Strategos strategos = new Strategos();

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate) {
		BuySellStrategy mamr = strategos.movingAvgMeanReverting(nPastDays, nHoldDays, threshold);

		return Read.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					int day = Arrays.binarySearch(dataSource.dates, To.string(backTestDate));
					GetBuySell gbs = mamr.analyze(dataSource.prices);
					int hold = 0;

					for (int i = 0; i < day; i++)
						hold += gbs.get(day);

					return (double) hold;
				}) //
				.toList();
	}

}
