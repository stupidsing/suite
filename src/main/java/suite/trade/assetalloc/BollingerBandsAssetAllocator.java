package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.stat.BollingerBands;
import suite.streamlet.Read;
import suite.trade.data.DataSource;

public class BollingerBandsAssetAllocator implements AssetAllocator {

	private int window = 32;
	private int k = 2;

	private BollingerBands bb = new BollingerBands();

	public static AssetAllocator of() {
		return AssetAllocator_.unleverage(new BollingerBandsAssetAllocator());
	}

	private BollingerBandsAssetAllocator() {
	}

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate) {
		return Read.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					float[] percentbs = bb.bb(dataSource.prices, window, k).percentb;
					int length = percentbs.length;
					double hold = 0d;
					for (int i = 0; i < length; i++) {
						float percentb = percentbs[i];
						if (percentb <= 0f)
							hold = 1d;
						else if (.5f < percentb) // un-short
							hold = 0d <= hold ? hold : 0d;
						else if (percentb < 1f) // un-long
							hold = hold < 0d ? hold : 0d;
						else
							hold = -1d;
					}
					return hold;
				}) //
				.toList();
	}

}
