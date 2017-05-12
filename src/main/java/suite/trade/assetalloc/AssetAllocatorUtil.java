package suite.trade.assetalloc;

public class AssetAllocatorUtil {

	public static AssetAllocator byTradeFrequency(AssetAllocator assetAllocator, int tradeFrequency) {
		return (dataSourceBySymbol, tradeDates, backTestDate) -> {
			if (backTestDate.toEpochDay() % tradeFrequency == 0)
				return assetAllocator.allocate(dataSourceBySymbol, tradeDates, backTestDate);
			else
				return null;
		};
	}

}
