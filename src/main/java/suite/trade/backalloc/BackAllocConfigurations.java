package suite.trade.backalloc;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.analysis.Factor;
import suite.trade.backalloc.strategy.BackAllocatorGeneral;
import suite.trade.backalloc.strategy.BackAllocatorMech;
import suite.trade.backalloc.strategy.BackAllocatorOld;
import suite.trade.backalloc.strategy.BackAllocator_;
import suite.trade.backalloc.strategy.MovingAvgMeanReversionBackAllocator0;
import suite.trade.backalloc.strategy.ReverseCorrelateBackAllocator;
import suite.trade.data.Configuration;
import suite.util.FunUtil.Fun;

public class BackAllocConfigurations {

	private Configuration cfg;
	private Fun<Time, Streamlet<Asset>> fun;

	public class Bacs {
		private Fun<Time, Streamlet<Asset>> fun_hsi = time -> Read.each(Asset.hsi);

		private BackAllocatorGeneral baGen = BackAllocatorGeneral.me;
		private BackAllocatorMech baMech = BackAllocatorMech.me;
		private BackAllocatorOld baOld = BackAllocatorOld.me;

		private BackAllocator ba_bbHold = baGen.bb_.filterByIndex(cfg).holdExtend(8);
		private BackAllocator ba_donHold = baGen.donHold;
		private BackAllocator ba_facoil = Factor.ofCrudeOil(cfg).backAllocator().longOnly().pick(3).even();

		public final BackAllocConfiguration bac_bbHold = ba_bbHold.cfgUnl(fun);
		public final BackAllocConfiguration bac_donHold = ba_donHold.cfgUnl(fun);
		public final BackAllocConfiguration bac_ema = baGen.ema.cfgUnl(fun);
		public final BackAllocConfiguration bac_pmamr = MovingAvgMeanReversionBackAllocator0.of().cfgUnl(fun);
		public final BackAllocConfiguration bac_pmmmr = baOld.movingMedianMeanRevn().holdExtend(9).cfgUnl(fun);
		public final BackAllocConfiguration bac_revco = ReverseCorrelateBackAllocator.of().cfgUnl(fun);
		public final BackAllocConfiguration bac_sell = baGen.cash.cfgUnl(fun);
		public final BackAllocConfiguration bac_tma = baGen.tma.cfgUnl(fun);

		private Streamlet2<String, BackAllocator> bas_ = baGen.baByName;
		private Streamlet2<String, BackAllocator> bas_mech = baMech.baByName.map2((n, ba) -> "me." + n, (n, ba) -> ba);

		private Streamlet2<String, BackAllocConfiguration> bacs_ = Streamlet2 //
				.concat(bas_, bas_mech) //
				.mapValue(ba -> ba.cfgUnl(fun));

		private Streamlet2<String, BackAllocConfiguration> bacByName0 = Read //
				.<String, BackAllocConfiguration> empty2() //
				.cons("hsi", BackAllocConfiguration.ofSingle(Asset.hsi)) //
				.cons("hsi.ppr", baGen.pprHsi.cfgUnl(fun_hsi)) //
				.cons("bb", bac_bbHold) //
				.cons("bbslope", baOld.bbSlope().cfgUnl(fun)) //
				.cons("facoil", ba_facoil.cfgUnl(fun)) //
				.cons("january", BackAllocator_.ofSingle(Asset.hsiSymbol).january().cfgUnl(fun_hsi)) //
				.cons("mix", BackAllocator_.sum(ba_bbHold, ba_donHold).cfgUnl(fun)) //
				.cons("pmamr", bac_pmamr) //
				.cons("pmmmr", bac_pmmmr) //
				.cons("revco", bac_revco) //
				.cons("revdd", baOld.revDrawdown().holdExtend(40).cfgUnl(fun)) //
				.cons("sellInMay", BackAllocator_.ofSingle(Asset.hsiSymbol).sellInMay().cfgUnl(fun_hsi));

		public final Streamlet2<String, BackAllocConfiguration> bacByName = Streamlet2 //
				.concat(bacs_, bacByName0);
	}

	public BackAllocConfigurations(Configuration cfg) {
		this(cfg, cfg::queryCompaniesByMarketCap);
	}

	public BackAllocConfigurations(Configuration cfg, Fun<Time, Streamlet<Asset>> fun) {
		super();
		this.cfg = cfg;
		this.fun = fun;
	}

	public Bacs bacs() {
		return new Bacs();
	}

}
