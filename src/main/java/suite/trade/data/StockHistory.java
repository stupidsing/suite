package suite.trade.data;

import static primal.statics.Fail.fail;
import static suite.util.Streamlet_.forInt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import primal.Ob;
import primal.String_;
import primal.fp.Funs.Iterate;
import primal.fp.Funs2.BinOp;
import suite.primitive.Int_Flt;
import suite.primitive.adt.pair.LngFltPair;
import suite.streamlet.As;
import suite.streamlet.Puller;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.DataSource.Datum;
import suite.util.Set_;

public class StockHistory {

	private static Cleanse cleanse = new Cleanse();

	public final String exchange;
	public final Time time;
	public final boolean isActive;
	public final Map<String, LngFltPair[]> data; // un-adjusted
	public final LngFltPair[] dividends;
	public final LngFltPair[] splits;

	public static StockHistory of(Puller<String> puller) {
		var properties = new HashMap<String, String>();
		var data = new HashMap<String, LngFltPair[]>();
		String line;

		while ('9' < (line = puller.pull()).charAt(0)) {
			var array = line.split("=");
			properties.put(array[0].trim(), array[1].trim());
		}

		var exchange = properties.get("exchange");
		var timeZoneStr = properties.get("timeZone");
		var isActive = properties.get("isActive");

		var timeZone = timeZoneStr != null ? Integer.parseInt(timeZoneStr) : 0;
		var dividends = readPairs(timeZone, puller);
		var splits = readPairs(timeZone, puller);
		String tag;

		while ((tag = puller.pull()) != null)
			data.put(tag, readPairs(timeZone, puller));

		return of( //
				exchange, //
				Time.ofYmdHms(line), //
				!String_.equals(isActive, "N"), //
				data, //
				dividends, //
				splits);
	}

	private static LngFltPair[] readPairs(int timeZone, Puller<String> puller) {
		var pairs = new ArrayList<LngFltPair>();
		String line;

		if (String_.equals(line = puller.pull(), "{"))
			while (!String_.equals(line = puller.pull(), "}")) {
				var p = line.lastIndexOf(":");
				var time = Time.of(line.substring(0, p));
				var price = Float.parseFloat(line.substring(p + 1));
				pairs.add(LngFltPair.of(time.epochSec(timeZone), price));
			}
		else
			fail();

		return pairs.toArray(new LngFltPair[0]);
	}

	public static StockHistory new_() {
		return of(null, TimeRange.min, true, new HashMap<>(), new LngFltPair[0], new LngFltPair[0]);
	}

	public static StockHistory of(//
			String exchange, //
			Time time, //
			boolean isActive, //
			Map<String, LngFltPair[]> data, //
			LngFltPair[] dividends, //
			LngFltPair[] splits) {
		return new StockHistory(exchange, time, isActive, data, dividends, splits);
	}

	private StockHistory( //
			String exchange, //
			Time time, //
			boolean isActive, //
			Map<String, LngFltPair[]> data, //
			LngFltPair[] dividends, //
			LngFltPair[] splits) {
		this.exchange = exchange;
		this.time = time;
		this.isActive = isActive;
		this.data = data;
		this.dividends = dividends;
		this.splits = splits;
	}

	public LngFltPair[] get(String tag) {
		return data.getOrDefault(tag, new LngFltPair[0]);
	}

	public StockHistory cleanse() {
		var data_ = Read //
				.from2(data) //
				.map2((name, pairs) -> {
					if (!String_.equals(name, "volume"))
						cleanse.cleanse(pairs);
					return pairs;
				}) //
				.toMap();

		return create(data_, dividends, splits);
	}

	public StockHistory filter(TimeRange period) {
		var t0 = period.fr.epochSec();
		var tx = period.to.epochSec();

		Iterate<LngFltPair[]> filter_ = pairs0 -> Read //
				.from(pairs0) //
				.filter(pair -> t0 <= pair.t0 && pair.t0 < tx) //
				.toArray(LngFltPair.class);

		var data1 = Read.from2(data).mapValue(filter_).toMap();

		return create(data1, filter_.apply(dividends), filter_.apply(splits));
	}

	public StockHistory merge(StockHistory other) {
		var isActive_ = isActive && other.isActive;
		var keys = Set_.union(data.keySet(), other.data.keySet());

		BinOp<LngFltPair[]> merge_ = (pairs0, pairs1) -> {
			var pairs = new ArrayList<LngFltPair>();
			var length1 = pairs1.length;
			var i1 = 0;
			for (var pair0 : pairs0) {
				var l0 = pair0.t0;
				while (i1 < length1) {
					var pair1 = pairs1[i1];
					var l1 = pair1.t0;
					if (l1 < l0)
						pairs.add(pair1);
					else if (l0 < l1)
						break;
					i1++;
				}
				pairs.add(pair0);
			}
			while (i1 < length1)
				pairs.add(pairs1[i1++]);
			return pairs.toArray(new LngFltPair[0]);
		};

		var data1 = Read //
				.from(keys) //
				.map2(key -> merge_.apply(get(key), other.get(key))) //
				.toMap();

		return create(isActive_, data1, merge_.apply(dividends, other.dividends), merge_.apply(splits, other.splits));
	}

	public StockHistory alignToDate() {
		Iterate<LngFltPair[]> align_ = pairs0 -> {
			var pairs1 = new ArrayList<LngFltPair>();
			var date = TimeRange.min;
			for (var pair : pairs0) {
				var date1 = Time.ofEpochSec(pair.t0).startOfDay();
				if (Ob.compare(date, date1) < 0)
					pairs1.add(pair);
				date = date1;
			}
			return pairs1.toArray(new LngFltPair[0]);
		};

		var data1 = Read.from2(data).mapValue(align_).toMap();

		return create(data1, align_.apply(dividends), align_.apply(splits));
	}

	public DataSource toDataSource() {
		var opPairs = adjustPrices("open");
		var clPairs = adjustPrices("close");
		var loPairs = adjustPrices("low");
		var hiPairs = adjustPrices("high");
		var vlPairs = data.get("volume");
		var ps = clPairs;
		var length = ps.length;

		var data = new Datum[length];
		int io = 0, ic = 0, il = 0, ih = 0, iv = 0;

		for (var i = 0; i < length; i++) {
			var t = ps[i].t0;
			int io_ = io, il_ = il, ih_ = ih, iv_ = iv;

			io = scan(opPairs, io, t);
			ic = scan(clPairs, ic, t);

			data[i] = new Datum( //
					t, //
					t + DataSource.tickDuration, //
					opPairs[io_].t1, //
					clPairs[ic - 1].t1, //
					forInt(il_, il = scan(loPairs, il_, t)).collect(Int_Flt.lift(i_ -> loPairs[i_].t1)).min(), //
					forInt(ih_, ih = scan(hiPairs, ih_, t)).collect(Int_Flt.lift(i_ -> hiPairs[i_].t1)).max(), //
					forInt(iv_, iv = scan(vlPairs, iv_, t)).collect(Int_Flt.lift(i_ -> vlPairs[i_].t1)).sum());
		}

		return DataSource.of(Read.from(data));
	}

	private int scan(LngFltPair[] pairs, int i, long t) {
		var length = pairs.length;
		while (i < length && pairs[i].t0 <= t)
			i++;
		return i;
	}

	public String write() {
		var s0 = Read.each( //
				"exchange = " + exchange, //
				"timeZone = 8", //
				time.ymdHms());
		var s1 = Read.each(dividends, splits).concatMap(this::concat);
		var s2 = Read.from2(data).concatMap((tag, fs) -> concat(fs).cons(tag));
		return Streamlet //
				.concat(s0, s1, s2) //
				.collect(As.joinedBy("\n"));
	}

	private Streamlet<String> concat(LngFltPair[] pairs) {
		return Streamlet.concat( //
				Read.each("{"), //
				Read.from(pairs).map(pair -> Time.ofEpochSec(pair.t0).ymdHms() + ":" + pair.t1), //
				Read.each("}"));
	}

	private LngFltPair[] adjustPrices(String tag) {
		var pairs0 = data.get(tag);
		var length = pairs0.length;
		var pairs1 = new LngFltPair[length];

		var si = splits.length - 1;
		var di = dividends.length - 1;
		float a = 0f, b = 1f;

		for (var i = length - 1; 0 <= i; i--) {
			var pair = pairs0[i];
			var t = pair.t0;

			if (0 <= di) {
				var dividend = dividends[di];
				if (t < dividend.t0) {
					if (Boolean.TRUE)
						// may got negative prices
						a -= dividend.t1 * b;
					else {
						// may got skewed profits
						b *= (pair.t1 - dividend.t1) / pair.t1;
					}
					di--;
				}
			}

			if (0 <= si) {
				var split = splits[si];
				if (t < split.t0) {
					a *= split.t1;
					b *= split.t1;
					si--;
				}
			}

			pairs1[i] = LngFltPair.of(pair.t0, a + b * pair.t1);
		}

		return pairs1;
	}

	public StockHistory create(Map<String, LngFltPair[]> data, LngFltPair[] dividends, LngFltPair[] splits) {
		return create(isActive, data, dividends, splits);
	}

	private StockHistory create( //
			boolean isActive, //
			Map<String, LngFltPair[]> data, //
			LngFltPair[] dividends, //
			LngFltPair[] splits) {
		return of(null, time, isActive, data, dividends, splits);
	}

}
