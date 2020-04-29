package suite.exchange;

public class ExSummary {

	public double vwapEntryPrice;
	public double unrealizedPnl;
	public double investedAmount;
	public double marginUsed;

	public String toString() {
		return "VWAP entry price = " + vwapEntryPrice
				+ ", unrealized PNL = " + unrealizedPnl
				+ ", invested amount = " + investedAmount
				+ ", margin used = " + marginUsed;
	}

}
