package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.stat.TimeSeries;
import suite.math.stat.TimeSeries.Donchian;
import suite.streamlet.Read;
import suite.trade.data.DataSource;

public class DonchianAssetAllocator implements AssetAllocator {

	private int window = 32;

	private TimeSeries ts = new TimeSeries();

	public static AssetAllocator of() {
		return AssetAllocator_.unleverage(new DonchianAssetAllocator());
	}

	private DonchianAssetAllocator() {
	}

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate) {
		return Read.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					Donchian donchian = ts.donchian(window, dataSource.prices);
					String[] dates = dataSource.dates;
					int length = dates.length;
					int length1 = length - 1;
					float price = dataSource.prices[length1];
					boolean hold = false;
					for (int i = 0; i < length; i++)
						if (price == donchian.mins[i])
							hold = true;
						else if (price == donchian.maxs[i])
							hold = false;
					return hold ? 1d : 0d;
				}) //
				.toList();
	}

}
