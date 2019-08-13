package suite.trade.backalloc;

import primal.MoreVerbs.Read;
import primal.fp.Funs.Fun;
import primal.streamlet.Streamlet;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.backalloc.strategy.BackAllocator_;

public class BackAllocConfiguration {

	public final Fun<Time, Streamlet<Instrument>> instrumentsFun;
	public final BackAllocator backAllocator;

	public static BackAllocConfiguration ofSingle(Instrument instrument) {
		return new BackAllocConfiguration(time -> Read.each(instrument), BackAllocator_.ofSingle(instrument.symbol));
	}

	public BackAllocConfiguration(Fun<Time, Streamlet<Instrument>> instrumentsFun, BackAllocator backAllocator) {
		this.instrumentsFun = instrumentsFun;
		this.backAllocator = backAllocator;
	}

}
