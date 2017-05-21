package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.streamlet.Read;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;

public class IndexRelativeAssetAllocator implements AssetAllocator {

	private AssetAllocator assetAllocator;
	private DataSource index;

	public static IndexRelativeAssetAllocator of(Configuration cfg, String indexSymbol, AssetAllocator assetAllocator) {
		DataSource index = cfg.dataSourceWithLatestQuote(indexSymbol);
		return new IndexRelativeAssetAllocator(index, assetAllocator);
	}

	private IndexRelativeAssetAllocator(DataSource index, AssetAllocator assetAllocator) {
		this.index = index;
		this.assetAllocator = assetAllocator;
	}

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol0, LocalDate backTestDate) {
		Map<String, DataSource> dataSourceBySymbol1 = Read.from2(dataSourceBySymbol0) //
				.mapValue(dataSource0 -> {
					String[] dates = dataSource0.dates;
					String[] indexDates = index.dates;
					float[] prices = dataSource0.prices;
					float[] indexPrices = index.prices;
					int length = dates.length;
					int indexLength = indexDates.length;
					float[] prices1 = new float[length];
					int ii = 0;
					for (int si = 0; si < length; si++) {
						String date = dates[si];
						while (ii < indexLength && indexDates[ii].compareTo(date) < 0)
							ii++;
						prices1[si] = prices[si] / indexPrices[ii];
					}
					return new DataSource(dates, prices1);
				}) //
				.toMap();

		return assetAllocator.allocate(dataSourceBySymbol1, backTestDate);
	}

}
