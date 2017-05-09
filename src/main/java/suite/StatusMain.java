package suite;

import java.util.Map;
import java.util.Set;

import suite.trade.Asset;
import suite.trade.data.Hkex;
import suite.trade.data.Summarize;
import suite.trade.data.Yahoo;
import suite.util.FunUtil.Fun;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.StatusMain
public class StatusMain extends ExecutableProgram {

	public static void main(String[] args) {
		Util.run(StatusMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Hkex hkex = new Hkex();
		Yahoo yahoo = new Yahoo();
		Fun<Set<String>, Map<String, Float>> quoteFun = yahoo::quote;
		Fun<String, Asset> getAssetFun = hkex::getCompany;
		Summarize summarize = new Summarize(quoteFun, getAssetFun);
		System.out.println(summarize.summarize(r -> r.strategy, s -> {
		}));
		return true;
	}

}
