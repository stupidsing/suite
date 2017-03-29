package suite.trade;

public class Account {

	private double cash = 0;
	private int nLots = 0;
	private int nTransactions = 0;

	public void buySell(int buySell, double price) {
		cash -= buySell * price;
		nLots += buySell;
		nTransactions += Math.abs(buySell);
	}

	public void validate() {
		if (cash < 0 || nLots < 0)
			throw new RuntimeException("invalid condition");
	}

	public double cash() {
		return cash;
	}

	public int nLots() {
		return nLots;
	}

	public int nTransactions() {
		return nTransactions;
	}

}
