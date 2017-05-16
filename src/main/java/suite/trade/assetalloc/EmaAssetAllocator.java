package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.streamlet.Read;
import suite.trade.MovingAverage;
import suite.trade.data.DataSource;

public class EmaAssetAllocator implements AssetAllocator {

	private MovingAverage movingAvg = new MovingAverage();

	public List<Pair<String, Double>> allocate( //
			Map<String, DataSource> dataSourceBySymbol, //
			List<LocalDate> tradeDates, //
			LocalDate backTestDate) {
		int halfLife = 32;
		double decay = Math.exp(Math.log(.5d) / halfLife);
		double threshold = .85d;

		return Read.from2(dataSourceBySymbol) //
				.map2((symbol, dataSource) -> symbol, (symbol, dataSource) -> {
					float[] ema = movingAvg.exponentialMovingAvg(dataSource.prices, decay);
					float lastEma = ema[ema.length - 1];
					float latest = dataSource.last().price;
					return latest / lastEma < threshold ? 1d : 0d;
				}) //
				.toList();
	}

}
