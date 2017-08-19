package suite.trade.backalloc;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.analysis.Factor;
import suite.trade.backalloc.strategy.BackAllocatorMech;
import suite.trade.backalloc.strategy.BackAllocatorOld_;
import suite.trade.backalloc.strategy.BackAllocator_;
import suite.trade.backalloc.strategy.MovingAvgMeanReversionBackAllocator0;
import suite.trade.backalloc.strategy.ReverseCorrelateBackAllocator;
import suite.trade.data.Configuration;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

public class BackAllocConfigurations {

	private Configuration cfg;
	private Sink<String> log;

	public final Bacs bacs;

	public class Bacs {
		private Fun<Time, Streamlet<Asset>> fun = cfg::queryCompaniesByMarketCap;
		private Fun<Time, Streamlet<Asset>> fun_hsi = time -> Read.each(Asset.hsi);

		private BackAllocator_ ba_ = BackAllocator_.me;
		private BackAllocatorMech ba_mech = BackAllocatorMech.me;
		private BackAllocatorOld_ ba_old = BackAllocatorOld_.me;

		private BackAllocator ba_bbHold = ba_.bb_.filterByIndex(cfg).holdExtend(8);
		private BackAllocator ba_donHold = ba_.donHold;
		private BackAllocator ba_facoil = Factor.ofCrudeOil(cfg).backAllocator().longOnly().pick(3).even();

		public final BackAllocConfiguration bac_bbHold = ba_bbHold.cfgUnl(fun);
		public final BackAllocConfiguration bac_donHold = ba_donHold.cfgUnl(fun);
		public final BackAllocConfiguration bac_ema = ba_.ema.cfgUnl(fun);
		public final BackAllocConfiguration bac_pmamr = MovingAvgMeanReversionBackAllocator0.of(log).cfgUnl(fun);
		public final BackAllocConfiguration bac_pmmmr = ba_old.movingMedianMeanRevn().holdExtend(9).cfgUnl(fun);
		public final BackAllocConfiguration bac_revco = ReverseCorrelateBackAllocator.of().cfgUnl(fun);
		public final BackAllocConfiguration bac_sell = ba_.cash.cfgUnl(fun);
		public final BackAllocConfiguration bac_tma = ba_.tma.cfgUnl(fun);

		private Streamlet2<String, BackAllocator> bas_ = ba_.baByName;
		private Streamlet2<String, BackAllocator> bas_mech = ba_mech.baByName.mapKey(n -> "me." + n);

		private Streamlet2<String, BackAllocConfiguration> bacs_ = Streamlet2 //
				.concat(bas_, bas_mech) //
				.mapValue(ba -> ba.cfgUnl(fun));

		private Streamlet2<String, BackAllocConfiguration> bacByName0 = Read //
				.<String, BackAllocConfiguration> empty2() //
				.cons("hsi", BackAllocConfiguration.ofSingle(Asset.hsi)) //
				.cons("hsi.shannon", ba_.shannon(Asset.hsiSymbol).cfgUnl(fun_hsi)) //
				.cons("bb", bac_bbHold) //
				.cons("bbSlope", ba_old.bbSlope().cfgUnl(fun)) //
				.cons("facoil", ba_facoil.cfgUnl(fun)) //
				.cons("january", ba_.ofSingle(Asset.hsiSymbol).january().cfgUnl(fun_hsi)) //
				.cons("mix", ba_.sum(ba_bbHold, ba_donHold).cfgUnl(fun)) //
				.cons("pmamr", bac_pmamr) //
				.cons("pmmmr", bac_pmmmr) //
				.cons("revco", bac_revco) //
				.cons("revdd", ba_old.revDrawdown().holdExtend(40).cfgUnl(fun)) //
				.cons("sellInMay", ba_.ofSingle(Asset.hsiSymbol).sellInMay().cfgUnl(fun_hsi));

		public final Streamlet2<String, BackAllocConfiguration> bacByName = Streamlet2 //
				.concat(bacs_, bacByName0);

		public BackAllocConfiguration questoaQuella(String symbol0, String symbol1) {
			Streamlet<Asset> assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);
			BackAllocator backAllocator = ba_old.questoQuella(symbol0, symbol1);
			return new BackAllocConfiguration(time -> assets, backAllocator);
		}
	}

	public BackAllocConfigurations(Configuration cfg, Sink<String> log) {
		this.cfg = cfg;
		this.log = log;
		bacs = new Bacs();
	}

}
