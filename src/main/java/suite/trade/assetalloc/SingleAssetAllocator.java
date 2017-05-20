package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.trade.data.DataSource;

public class SingleAssetAllocator implements AssetAllocator {

	private String symbol;

	public SingleAssetAllocator(String symbol) {
		this.symbol = symbol;
	}

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate) {
		return Arrays.asList(Pair.of(symbol, 1d));
	}

}
