package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.trade.data.DataSource;

public class SingleAssetAllocator implements AssetAllocator {

	private String symbol;

	public SingleAssetAllocator(String symbol) {
		this.symbol = symbol;
	}

	public OnDate allocate(Map<String, DataSource> dataSourceBySymbol, List<LocalDate> tradeDates) {
		return backTestDate -> Arrays.asList(Pair.of(symbol, 1d));
	}

}
