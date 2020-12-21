package suite.trade.data;

import static primal.statics.Rethrow.ex;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.fp.Funs.Fun;
import primal.primitive.IntPrim.Obj_Int;
import primal.streamlet.Streamlet;
import suite.http.HttpClient;
import suite.node.util.Singleton;
import suite.os.Execute;
import suite.os.SerializedStoreCache;
import suite.serialize.Serialize;
import suite.trade.Instrument;
import suite.util.To;

public class Hkex {

	private static ObjectMapper om = new ObjectMapper();
	// .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	private Serialize ser = Singleton.me.serialize;
	private Sina sina = new Sina();

	private Fun<Streamlet<String>, Map<String, Integer>> queryLotSizes = symbols -> sina.queryLotSizes(symbols, true);

	// https://www.hkex.com.hk/eng/csm/result.htm?location=companySearch&SearchMethod=2&mkt=hk&LangCode=en&StockType=MB&Ranking=ByMC&x=42&y=9
	// stock code, stock name, market capitalisation (million)
	private String lines = """
					0700.HK|Tencent Holdings Ltd.|100|2373162
					0941.HK|China Mobile Ltd.|500|1749630
					0939.HK|China Construction Bank Corporation - H Shares|1000|1524246
					0005.HK|HSBC Holdings plc|400|1372460
					1299.HK|AIA Group Ltd.|200|671838
					2378.HK|Prudential plc|500|456381
					1398.HK|Industrial and Commercial Bank of China Ltd. - H Shares|1000|442650
					0883.HK|CNOOC Ltd.|1000|397362
					0001.HK|CK Hutchison Holdings Ltd.|500|388468
					0805.HK|Glencore plc|100|383312
					2388.HK|BOC Hong Kong (Holdings) Ltd.|500|346787
					2318.HK|Ping An Insurance (Group) Co. of China, Ltd. - H Shares|500|338865
					0016.HK|Sun Hung Kai Properties Ltd.|1000|338234
					0267.HK|CITIC Ltd.|1000|330465
					3988.HK|Bank of China Ltd. - H Shares|1000|316928
					0011.HK|Hang Seng Bank Ltd.|100|311057
					1928.HK|Sands China Ltd.|400|279312
					0066.HK|MTR Corporation Ltd.|500|275937
					0945.HK|Manulife Financial Corporation|100|274569
					0762.HK|China Unicom (Hong Kong) Ltd.|2000|248571
					0688.HK|China Overseas Land & Investment Ltd.|2000|241584
					2888.HK|Standard Chartered PLC|50|239536
					0388.HK|Hong Kong Exchanges and Clearing Ltd.|100|237763
					1113.HK|Cheung Kong Property Holdings Ltd.|500|211492
					0002.HK|CLP Holdings Ltd.|500|211464
					3328.HK|Bank of Communications Co., Ltd. - H Shares|1000|206220
					0004.HK|Wharf (Holdings) Ltd., The|1000|202620
					0003.HK|Hong Kong and China Gas Co. Ltd., The|1000|199133
					1038.HK|Cheung Kong Infrastructure Holdings Ltd.|1000|183427
					0012.HK|Henderson Land Development Co. Ltd.|1000|180233
					2628.HK|China Life Insurance Co. Ltd. - H Shares|1000|179704
					0027.HK|Galaxy Entertainment Group Ltd.|1000|172605
					0566.HK|Hanergy Thin Film Power Group Ltd.|2000|163669
					2007.HK|Country Garden Holdings Co. Ltd.|1000|162503
					0386.HK|China Petroleum & Chemical Corporation - H Shares|2000|158438
					0006.HK|Power Assets Holdings Ltd.|500|152706
					1972.HK|Swire Properties Ltd.|200|148298
					1109.HK|China Resources Land Ltd.|2000|142777
					2018.HK|AAC Technologies Holdings Inc.|500|136308
					0020.HK|Wheelock and Co. Ltd.|1000|123752
					0857.HK|PetroChina Co. Ltd. - H Shares|2000|111613
					1288.HK|Agricultural Bank of China Ltd. - H Shares|1000|111275
					3333.HK|China Evergrande Group|1000|106232
					0656.HK|Fosun International Ltd.|500|100546
					0288.HK|WH Group Ltd.|500|99920
					1658.HK|Postal Savings Bank of China Co., Ltd. - H Shares|1000|98884
					0175.HK|Geely Automobile Holdings Ltd.|5000|97545
					0017.HK|New World Development Co. Ltd.|1000|93883
					3968.HK|China Merchants Bank Co., Ltd. - H Shares|500|93195
					1913.HK|PRADA S.p.A.|100|89303
					0023.HK|Bank of East Asia, Ltd., The|200|89056
					0101.HK|Hang Lung Properties Ltd.|1000|88332
					1929.HK|Chow Tai Fook Jewellery Group Ltd.|200|85700
					0083.HK|Sino Land Co. Ltd.|2000|84629
					2799.HK|China Huarong Asset Management Co., Ltd. - H Shares|1000|82144
					1128.HK|Wynn Macau, Ltd.|400|81570
					2601.HK|China Pacific Insurance (Group) Co., Ltd. - H Shares|200|79235
					0960.HK|Longfor Properties Co. Ltd.|500|77511
					6823.HK|HKT Trust and HKT Ltd. - SS|1000|75490
					2313.HK|Shenzhou International Group Holdings Ltd.|1000|74077
					0270.HK|Guangdong Investment Ltd.|2000|73616
					2382.HK|Sunny Optical Technology (Group) Co. Ltd.|1000|73499
					0998.HK|China CITIC Bank Corporation Ltd. - H Shares|1000|72625
					0966.HK|China Taiping Insurance Holdings Co. Ltd.|200|70730
					1044.HK|Hengan International Group Co. Ltd.|500|69582
					6808.HK|Sun Art Retail Group Ltd.|500|68686
					1093.HK|CSPC Pharmaceutical Group Ltd.|2000|68531
					0836.HK|China Resources Power Holdings Co. Ltd.|2000|67346
					0019.HK|Swire Pacific Ltd. 'A'|500|66940
					0607.HK|Fullshare Holdings Ltd.|2500|66684
					1114.HK|Brilliance China Automotive Holdings Ltd.|2000|66425
					0151.HK|Want Want China Holdings Ltd.|1000|66325
					0669.HK|Techtronic Industries Co. Ltd.|500|64703
					2282.HK|MGM China Holdings Ltd.|400|64221
					2638.HK|HK Electric Investments and HK Electric Investments Ltd. -SS|500|62649
					0384.HK|China Gas Holdings Ltd.|2000|62206
					1088.HK|China Shenhua Energy Co. Ltd. - H Shares|500|61446
					3799.HK|Dali Foods Group Co. Ltd.|500|61213
					3311.HK|China State Construction International Holdings Ltd.|2000|61128
					0291.HK|China Resources Beer (Holdings) Co. Ltd.|2000|60212
					0659.HK|NWS Holdings Ltd.|1000|58788
					2328.HK|PICC Property and Casualty Co. Ltd. - H Shares|2000|58138
					2020.HK|ANTA Sports Products Ltd.|1000|57972
					3320.HK|China Resources Pharmaceutical Group Ltd.|500|57755
					2319.HK|China Mengniu Dairy Co. Ltd.|1000|57698
					0144.HK|China Merchants Port Holdings Co. Ltd.|2000|57504
					1193.HK|China Resources Gas Group Ltd.|2000|57491
					0486.HK|United Company RUSAL Plc|1000|56518
					0135.HK|Kunlun Energy Co. Ltd.|2000|56103
					0992.HK|Lenovo Group Ltd.|2000|55099
					0322.HK|Tingyi (Cayman Islands) Holding Corp.|2000|54423
					0551.HK|Yue Yuen Industrial (Holdings) Ltd.|500|54332
					1988.HK|China Minsheng Banking Corp., Ltd. - H Shares|500|52973
					0728.HK|China Telecom Corporation Ltd. - H Shares|2000|52595
					0371.HK|Beijing Enterprises Water Group Ltd.|2000|52424
					1169.HK|Haier Electronics Group Co., Ltd.|1000|51782
					1880.HK|Belle International Holdings Ltd.|1000|51196
					1378.HK|China Hongqiao Group Ltd.|500|51181
					0522.HK|ASM Pacific Technology Ltd.|100|49928
					1800.HK|China Communications Construction Co. Ltd. - H Shares|1000|48083
					0392.HK|Beijing Enterprises Holdings Ltd.|500|46889
					1177.HK|Sino Biopharmaceutical Ltd.|1000|46845
					2098.HK|Zall Group Ltd.|3000|46528
					2688.HK|ENN Energy Holdings Ltd.|1000|46028
					0257.HK|China Everbright International Ltd.|1000|44827
					1910.HK|Samsonite International S.A.|300|44515
					0010.HK|Hang Lung Group Ltd.|1000|44116
					1357.HK|Meitu, Inc.|500|43636
					0981.HK|Semiconductor Manufacturing International Corporation|500|43568
					6837.HK|Haitong Securities Co., Ltd. - H Shares|400|43029
					0293.HK|Cathay Pacific Airways Ltd.|1000|42800
					2689.HK|Nine Dragons Paper (Holdings) Ltd.|1000|42769
					0683.HK|Kerry Properties Ltd.|500|42143
					1336.HK|New China Life Insurance Co. Ltd. - H Shares|100|42088
					1099.HK|Sinopharm Group Co. Ltd. - H Shares|400|41092
					0247.HK|Tsim Sha Tsui Properties Ltd.|2000|40488
					0880.HK|SJM Holdings Ltd.|1000|40449
					0069.HK|Shangri-La Asia Ltd.|2000|40383
					1211.HK|BYD Co. Ltd. - H Shares|500|40169
					0813.HK|Shimao Property Holdings Ltd.|500|39764
			""";

	private Streamlet<Instrument> companies = Read //
			.from(lines.split("\n")) //
			.filter(line -> !line.isEmpty()) //
			.map(line -> line.split("\\|")) //
			.map(array -> Instrument.of(array[0], array[1], Integer.parseInt(array[2]), Integer.parseInt(array[3]))) //
			.collect();

	private Map<String, Instrument> companyBySymbol = Read.from(companies).toMap(company -> company.symbol);

	private Set<String> delisted = new HashSet<>(List.of("0013.HK", "0020.HK"));
	private Obj_Int<String> queryLotSize = sina::queryLotSize;

	public static final Set<String> commonFirstNames = new HashSet<>(
			List.of("", "China", "Guangdong", "Hang", "HK", "Hongkong", "New", "Standard"));

	public static class Data {
		public static class Content {
			@JsonIgnoreProperties(ignoreUnknown = true)
			public static class Table {
				public static class Tr {
					public boolean thead;
					public List<List<String>> td;
					public String link;
				}

				public String classname;
				public String title;
				public List<String> colWidth;
				public List<String> colAlign;
				public List<Tr> tr;
			}

			public int style;
			public String classname;
			public JsonNode table;
			private List<Table> tables_;

			private List<Table> getTables() {
				return tables_ != null ? tables_ : (tables_ = tables_());
			}

			private List<Table> tables_() {
				if (table.isArray())
					return Read.from(table).map(json -> om.convertValue(json, Table.class)).toList();
				else
					return List.of(om.convertValue(table, Table.class));
			}
		}

		public int id;
		public String tabName;
		public String title;
		public List<Content> content;
		public String remark;

		private Streamlet<List<String>> tableEntries() {
			return Read //
					.from(content) //
					.flatMap(Content::getTables) //
					.flatMap(table -> table.tr) //
					.filter(tr -> !tr.thead) //
					.flatMap(tr -> tr.td);
		}
	}

	public static class CompanySearch {
		public String IndexName;
		public List<Data> data;
		public String PageNo;
		public int PageSize;
		public int TotalCount;
		public String LastUpdateDate;
	}

	public static class CompanyInfo {
		public String stockName;
		public String stockCode;
		public List<Data> data;
	}

	public Streamlet<Instrument> getCompanies() {
		return companies;
	}

	public Instrument queryCompany(String symbol) {
		return !Equals.string(symbol, "6098.HK") ? queryCompany_(symbol)
				: Instrument.of("6098.HK", "CG SERVICES", 1000);
	}

	private Instrument queryCompany_(String symbol) {
		var instrument = companyBySymbol.get(symbol);

		if (instrument == null && !delisted.contains(symbol)) {
			var json = query("" //
					+ "https://www.hkex.com.hk/eng/csm/ws/Company.asmx/GetData" //
					+ "?location=companySearch" //
					+ "&SearchMethod=1" //
					+ "&LangCode=en" //
					+ "&StockCode=" + HkexUtil.toStockCode(symbol) //
					+ "&StockName=" //
					+ "&mkt=hk" //
					+ "&x=" //
					+ "&y=");

			var companyInfo = om.convertValue(json, CompanyInfo.class);

			instrument = Instrument.of( //
					HkexUtil.toSymbol(companyInfo.stockCode), //
					companyInfo.stockName.split("\\[")[0].trim(), //
					queryLotSize.apply(symbol));
		}

		return instrument;
	}

	public Streamlet<Instrument> queryCompanies() {
		return Read //
				.each(queryCompanies(0), queryCompanies(1), queryCompanies(2)) //
				.flatMap(list -> list);
	}

	private List<Instrument> queryCompanies(int pageNo) {
		return SerializedStoreCache //
				.of(ser.list(Instrument.serializer)) //
				.get(getClass().getSimpleName() + ".queryCompanies(" + pageNo + ")", () -> queryCompanies_(pageNo));
	}

	public float queryHangSengIndex() {
		var url = "https://www.hkex.com.hk/eng/csm/ws/IndexMove.asmx/GetData?LangCode=en";

		return HttpClient.get(url).inputStream().doRead(is -> Read //
				.each(om.readTree(is)) //
				.flatMap(json_ -> json_.path("data")) //
				.filter(json_ -> Equals.string(json_.path("title").textValue(), "Hong Kong")) //
				.flatMap(json_ -> json_.path("content")) //
				.filter(json_ -> Equals.string(json_.path(0).textValue(), "Hang Seng Index")) //
				.map(json_ -> Float.parseFloat(json_.path(1).textValue().split(" ")[0].replace(",", ""))) //
				.uniqueResult());
	}

	// https://www.daytradetheworld.com/wiki/hkex/
	public float getTickSize(float price) {
		if (price <= .25f)
			return .001f;
		else if (price <= .5f)
			return .005f;
		else if (price <= 10f)
			return .01f;
		else if (price <= 20f)
			return .02f;
		else if (price <= 100f)
			return .05f;
		else if (price <= 200f)
			return .1f;
		else if (price <= 500f)
			return .2f;
		else if (price <= 1000f)
			return .5f;
		else if (price <= 2000f)
			return 1f;
		else if (price <= 5000f)
			return 2f;
		else if (price <= 9995f)
			return 5f;
		else
			return Float.NaN;
	}

	private List<Instrument> queryCompanies_(int pageNo) {
		var json = query("" //
				+ "https://www.hkex.com.hk/eng/csm/ws/Result.asmx/GetData" //
				+ "?location=companySearch" //
				+ "&SearchMethod=2" //
				+ "&LangCode=en" //
				+ "&StockCode=" //
				+ "&StockName=" //
				+ "&Ranking=ByMC" //
				+ "&StockType=MB" //
				+ "&mkt=hk" //
				+ "&PageNo=" + (pageNo + 1) //
				+ "&ATypeSHEx=" //
				+ "&AType=" //
				+ "&FDD=" //
				+ "&FMM=" //
				+ "&FYYYY=" //
				+ "&TDD=" //
				+ "&TMM=" //
				+ "&TYYYY=");

		var companySearch = om.convertValue(json, CompanySearch.class);
		Streamlet<List<String>> data0;

		if (Boolean.TRUE)
			data0 = Read //
					.each(companySearch) //
					.flatMap(cs -> cs.data) //
					.concatMap(Data::tableEntries);
		else
			data0 = Read //
					.each(json) //
					.flatMap(json_ -> json_.path("data")) //
					.flatMap(json_ -> json_.path("content")) //
					.flatMap(json_ -> json_.path("table")) //
					.flatMap(json_ -> json_.path("tr")) //
					.filter(json_ -> !json_.path("thead").asBoolean()) //
					.flatMap(json_ -> json_.path("td")) //
					.map(json_ -> Read.from(json_).map(JsonNode::textValue).toList());

		var data1 = data0.collect();
		var lotSizeBySymbol = queryLotSizes.apply(data1.map(this::toSymbol));
		return data1.map(datum -> toinstrument(datum, lotSizeBySymbol)).toList();
	}

	private JsonNode query(String url) {
		JsonNode json;

		if (Boolean.TRUE)
			json = Singleton.me.storeCache.http(url).collect(To::inputStream).doRead(om::readTree);
		else {
			var execute = new Execute(new String[] { "curl", url, });
			json = ex(() -> om.readTree(execute.out));
		}

		return json;
	}

	private Instrument toinstrument(List<String> list, Map<String, Integer> lotSizeBySymbol) {
		var symbol = toSymbol(list);
		return Instrument.of( //
				symbol, //
				list.get(2).trim(), //
				lotSizeBySymbol.get(symbol), //
				Integer.parseInt(list.get(3).substring(4).replace("\n", "").replace(",", "").trim()));
	}

	private String toSymbol(List<String> list) {
		return HkexUtil.toSymbol(list.get(1).replace("*", ""));
	}

}
