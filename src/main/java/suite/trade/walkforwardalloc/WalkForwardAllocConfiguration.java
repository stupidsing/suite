package suite.trade.walkforwardalloc;

import primal.streamlet.Streamlet;
import suite.trade.Instrument;

public class WalkForwardAllocConfiguration {

	public final Streamlet<Instrument> instruments;
	public final WalkForwardAllocator walkForwardAllocator;

	public WalkForwardAllocConfiguration(Streamlet<Instrument> instruments, WalkForwardAllocator walkForwardAllocator) {
		this.instruments = instruments;
		this.walkForwardAllocator = walkForwardAllocator;
	}

}
