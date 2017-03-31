package suite.trade;

public class Account {

	private float cash = 0;
	private int nLots = 0;
	private int nTransactions = 0;

	public void buySell(int buySell, float price) {
		cash -= buySell * price;
		nLots += buySell;
		nTransactions += Math.abs(buySell);
	}

	public void validate() {
		if (cash < 0 || nLots < 0)
			throw new RuntimeException("invalid condition");
	}

	public float cash() {
		return cash;
	}

	public int nLots() {
		return nLots;
	}

	public int nTransactions() {
		return nTransactions;
	}

}
