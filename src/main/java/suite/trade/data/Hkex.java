package suite.trade.data;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import suite.http.HttpUtil;
import suite.node.util.Singleton;
import suite.os.Execute;
import suite.os.SerializedStoreCache;
import suite.serialize.Serialize;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.util.Fail;
import suite.util.FunUtil.Source;
import suite.util.Rethrow;
import suite.util.String_;
import suite.util.To;

public class Hkex {

	// .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	private static ObjectMapper mapper = new ObjectMapper();
	private Serialize serialize = Singleton.me.serialize;

	private Set<String> delisted = new HashSet<>(List.of("0013.HK"));

	// https://www.hkex.com.hk/eng/csm/result.htm?location=companySearch&SearchMethod=2&mkt=hk&LangCode=en&StockType=MB&Ranking=ByMC&x=42&y=9
	// stock code, stock name, market capitalisation (million)
	private String lines = "" //
			+ "\n0700.HK|Tencent Holdings Ltd.|100|2373162" //
			+ "\n0941.HK|China Mobile Ltd.|500|1749630" //
			+ "\n0939.HK|China Construction Bank Corporation - H Shares|1000|1524246" //
			+ "\n0005.HK|HSBC Holdings plc|400|1372460" //
			+ "\n1299.HK|AIA Group Ltd.|200|671838" //
			+ "\n2378.HK|Prudential plc|500|456381" //
			+ "\n1398.HK|Industrial and Commercial Bank of China Ltd. - H Shares|1000|442650" //
			+ "\n0883.HK|CNOOC Ltd.|1000|397362" //
			+ "\n0001.HK|CK Hutchison Holdings Ltd.|500|388468" //
			+ "\n0805.HK|Glencore plc|100|383312" //
			+ "\n2388.HK|BOC Hong Kong (Holdings) Ltd.|500|346787" //
			+ "\n2318.HK|Ping An Insurance (Group) Co. of China, Ltd. - H Shares|500|338865" //
			+ "\n0016.HK|Sun Hung Kai Properties Ltd.|1000|338234" //
			+ "\n0267.HK|CITIC Ltd.|1000|330465" //
			+ "\n3988.HK|Bank of China Ltd. - H Shares|1000|316928" //
			+ "\n0011.HK|Hang Seng Bank Ltd.|100|311057" //
			+ "\n1928.HK|Sands China Ltd.|400|279312" //
			+ "\n0066.HK|MTR Corporation Ltd.|500|275937" //
			+ "\n0945.HK|Manulife Financial Corporation|100|274569" //
			+ "\n0762.HK|China Unicom (Hong Kong) Ltd.|2000|248571" //
			+ "\n0688.HK|China Overseas Land & Investment Ltd.|2000|241584" //
			+ "\n2888.HK|Standard Chartered PLC|50|239536" //
			+ "\n0388.HK|Hong Kong Exchanges and Clearing Ltd.|100|237763" //
			+ "\n1113.HK|Cheung Kong Property Holdings Ltd.|500|211492" //
			+ "\n0002.HK|CLP Holdings Ltd.|500|211464" //
			+ "\n3328.HK|Bank of Communications Co., Ltd. - H Shares|1000|206220" //
			+ "\n0004.HK|Wharf (Holdings) Ltd., The|1000|202620" //
			+ "\n0003.HK|Hong Kong and China Gas Co. Ltd., The|1000|199133" //
			+ "\n1038.HK|Cheung Kong Infrastructure Holdings Ltd.|1000|183427" //
			+ "\n0012.HK|Henderson Land Development Co. Ltd.|1000|180233" //
			+ "\n2628.HK|China Life Insurance Co. Ltd. - H Shares|1000|179704" //
			+ "\n0027.HK|Galaxy Entertainment Group Ltd.|1000|172605" //
			+ "\n0566.HK|Hanergy Thin Film Power Group Ltd.|2000|163669" //
			+ "\n2007.HK|Country Garden Holdings Co. Ltd.|1000|162503" //
			+ "\n0386.HK|China Petroleum & Chemical Corporation - H Shares|2000|158438" //
			+ "\n0006.HK|Power Assets Holdings Ltd.|500|152706" //
			+ "\n1972.HK|Swire Properties Ltd.|200|148298" //
			+ "\n1109.HK|China Resources Land Ltd.|2000|142777" //
			+ "\n2018.HK|AAC Technologies Holdings Inc.|500|136308" //
			+ "\n0020.HK|Wheelock and Co. Ltd.|1000|123752" //
			+ "\n0857.HK|PetroChina Co. Ltd. - H Shares|2000|111613" //
			+ "\n1288.HK|Agricultural Bank of China Ltd. - H Shares|1000|111275" //
			+ "\n3333.HK|China Evergrande Group|1000|106232" //
			+ "\n0656.HK|Fosun International Ltd.|500|100546" //
			+ "\n0288.HK|WH Group Ltd.|500|99920" //
			+ "\n1658.HK|Postal Savings Bank of China Co., Ltd. - H Shares|1000|98884" //
			+ "\n0175.HK|Geely Automobile Holdings Ltd.|5000|97545" //
			+ "\n0017.HK|New World Development Co. Ltd.|1000|93883" //
			+ "\n3968.HK|China Merchants Bank Co., Ltd. - H Shares|500|93195" //
			+ "\n1913.HK|PRADA S.p.A.|100|89303" //
			+ "\n0023.HK|Bank of East Asia, Ltd., The|200|89056" //
			+ "\n0101.HK|Hang Lung Properties Ltd.|1000|88332" //
			+ "\n1929.HK|Chow Tai Fook Jewellery Group Ltd.|200|85700" //
			+ "\n0083.HK|Sino Land Co. Ltd.|2000|84629" //
			+ "\n2799.HK|China Huarong Asset Management Co., Ltd. - H Shares|1000|82144" //
			+ "\n1128.HK|Wynn Macau, Ltd.|400|81570" //
			+ "\n2601.HK|China Pacific Insurance (Group) Co., Ltd. - H Shares|200|79235" //
			+ "\n0960.HK|Longfor Properties Co. Ltd.|500|77511" //
			+ "\n6823.HK|HKT Trust and HKT Ltd. - SS|1000|75490" //
			+ "\n2313.HK|Shenzhou International Group Holdings Ltd.|1000|74077" //
			+ "\n0270.HK|Guangdong Investment Ltd.|2000|73616" //
			+ "\n2382.HK|Sunny Optical Technology (Group) Co. Ltd.|1000|73499" //
			+ "\n0998.HK|China CITIC Bank Corporation Ltd. - H Shares|1000|72625" //
			+ "\n0966.HK|China Taiping Insurance Holdings Co. Ltd.|200|70730" //
			+ "\n1044.HK|Hengan International Group Co. Ltd.|500|69582" //
			+ "\n6808.HK|Sun Art Retail Group Ltd.|500|68686" //
			+ "\n1093.HK|CSPC Pharmaceutical Group Ltd.|2000|68531" //
			+ "\n0836.HK|China Resources Power Holdings Co. Ltd.|2000|67346" //
			+ "\n0019.HK|Swire Pacific Ltd. 'A'|500|66940" //
			+ "\n0607.HK|Fullshare Holdings Ltd.|2500|66684" //
			+ "\n1114.HK|Brilliance China Automotive Holdings Ltd.|2000|66425" //
			+ "\n0151.HK|Want Want China Holdings Ltd.|1000|66325" //
			+ "\n0669.HK|Techtronic Industries Co. Ltd.|500|64703" //
			+ "\n2282.HK|MGM China Holdings Ltd.|400|64221" //
			+ "\n2638.HK|HK Electric Investments and HK Electric Investments Ltd. -SS|500|62649" //
			+ "\n0384.HK|China Gas Holdings Ltd.|2000|62206" //
			+ "\n1088.HK|China Shenhua Energy Co. Ltd. - H Shares|500|61446" //
			+ "\n3799.HK|Dali Foods Group Co. Ltd.|500|61213" //
			+ "\n3311.HK|China State Construction International Holdings Ltd.|2000|61128" //
			+ "\n0291.HK|China Resources Beer (Holdings) Co. Ltd.|2000|60212" //
			+ "\n0659.HK|NWS Holdings Ltd.|1000|58788" //
			+ "\n2328.HK|PICC Property and Casualty Co. Ltd. - H Shares|2000|58138" //
			+ "\n2020.HK|ANTA Sports Products Ltd.|1000|57972" //
			+ "\n3320.HK|China Resources Pharmaceutical Group Ltd.|500|57755" //
			+ "\n2319.HK|China Mengniu Dairy Co. Ltd.|1000|57698" //
			+ "\n0144.HK|China Merchants Port Holdings Co. Ltd.|2000|57504" //
			+ "\n1193.HK|China Resources Gas Group Ltd.|2000|57491" //
			+ "\n0486.HK|United Company RUSAL Plc|1000|56518" //
			+ "\n0135.HK|Kunlun Energy Co. Ltd.|2000|56103" //
			+ "\n0992.HK|Lenovo Group Ltd.|2000|55099" //
			+ "\n0322.HK|Tingyi (Cayman Islands) Holding Corp.|2000|54423" //
			+ "\n0551.HK|Yue Yuen Industrial (Holdings) Ltd.|500|54332" //
			+ "\n1988.HK|China Minsheng Banking Corp., Ltd. - H Shares|500|52973" //
			+ "\n0728.HK|China Telecom Corporation Ltd. - H Shares|2000|52595" //
			+ "\n0371.HK|Beijing Enterprises Water Group Ltd.|2000|52424" //
			+ "\n1169.HK|Haier Electronics Group Co., Ltd.|1000|51782" //
			+ "\n1880.HK|Belle International Holdings Ltd.|1000|51196" //
			+ "\n1378.HK|China Hongqiao Group Ltd.|500|51181" //
			+ "\n0522.HK|ASM Pacific Technology Ltd.|100|49928" //
			+ "\n1800.HK|China Communications Construction Co. Ltd. - H Shares|1000|48083" //
			+ "\n0392.HK|Beijing Enterprises Holdings Ltd.|500|46889" //
			+ "\n1177.HK|Sino Biopharmaceutical Ltd.|1000|46845" //
			+ "\n2098.HK|Zall Group Ltd.|3000|46528" //
			+ "\n2688.HK|ENN Energy Holdings Ltd.|1000|46028" //
			+ "\n0257.HK|China Everbright International Ltd.|1000|44827" //
			+ "\n1910.HK|Samsonite International S.A.|300|44515" //
			+ "\n0010.HK|Hang Lung Group Ltd.|1000|44116" //
			+ "\n1357.HK|Meitu, Inc.|500|43636" //
			+ "\n0981.HK|Semiconductor Manufacturing International Corporation|500|43568" //
			+ "\n6837.HK|Haitong Securities Co., Ltd. - H Shares|400|43029" //
			+ "\n0293.HK|Cathay Pacific Airways Ltd.|1000|42800" //
			+ "\n2689.HK|Nine Dragons Paper (Holdings) Ltd.|1000|42769" //
			+ "\n0683.HK|Kerry Properties Ltd.|500|42143" //
			+ "\n1336.HK|New China Life Insurance Co. Ltd. - H Shares|100|42088" //
			+ "\n1099.HK|Sinopharm Group Co. Ltd. - H Shares|400|41092" //
			+ "\n0247.HK|Tsim Sha Tsui Properties Ltd.|2000|40488" //
			+ "\n0880.HK|SJM Holdings Ltd.|1000|40449" //
			+ "\n0069.HK|Shangri-La Asia Ltd.|2000|40383" //
			+ "\n1211.HK|BYD Co. Ltd. - H Shares|500|40169" //
			+ "\n0813.HK|Shimao Property Holdings Ltd.|500|39764" //
	;

	private Streamlet<Asset> companies = Read //
			.from(lines.split("\n")) //
			.filter(line -> !line.isEmpty()) //
			.map(line -> line.split("\\|")) //
			.map(array -> Asset.of(array[0], array[1], Integer.parseInt(array[2]), Integer.parseInt(array[3]))) //
			.collect();

	private Map<String, Asset> companyBySymbol = Read.from(companies).toMap(company -> company.symbol);

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
					return Read.from(table).map(json -> mapper.convertValue(json, Table.class)).toList();
				else
					return List.of(mapper.convertValue(table, Table.class));
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

	public Streamlet<Asset> getCompanies() {
		return companies;
	}

	public Asset queryCompany(String symbol) {
		return !String_.equals(symbol, "6098.HK") ? queryCompany_(symbol) : Asset.of("6098.HK", "CG SERVICES", 1000);
	}

	private Asset queryCompany_(String symbol) {
		var asset = companyBySymbol.get(symbol);

		if (asset == null && !delisted.contains(symbol)) {
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

			var companyInfo = mapper.convertValue(json, CompanyInfo.class);

			asset = Asset.of( //
					HkexUtil.toSymbol(companyInfo.stockCode), //
					companyInfo.stockName.split("\\[")[0].trim(), //
					queryBoardLot(symbol));
		}

		return asset;
	}

	public Streamlet<Asset> queryCompanies() {
		return Read //
				.each(queryCompanies(0), queryCompanies(1), queryCompanies(2)) //
				.flatMap(list -> list);
	}

	private List<Asset> queryCompanies(int pageNo) {
		return SerializedStoreCache //
				.of(serialize.list(Asset.serializer)) //
				.get(getClass().getSimpleName() + ".queryCompanies(" + pageNo + ")", () -> queryCompanies_(pageNo));
	}

	public int queryBoardLot(String symbol) {
		return queryBoardLot_(symbol);
	}

	public float queryHangSengIndex() {
		var url = "https://www.hkex.com.hk/eng/csm/ws/IndexMove.asmx/GetData?LangCode=en";

		try (var is = HttpUtil.get(url).inputStream()) {
			return Read //
					.each(mapper.readTree(is)) //
					.flatMap(json_ -> json_.path("data")) //
					.filter(json_ -> String_.equals(json_.path("title").textValue(), "Hong Kong")) //
					.flatMap(json_ -> json_.path("content")) //
					.filter(json_ -> String_.equals(json_.path(0).textValue(), "Hang Seng Index")) //
					.map(json_ -> Float.parseFloat(json_.path(1).textValue().split(" ")[0].replace(",", ""))) //
					.uniqueResult();
		} catch (IOException ex) {
			return Fail.t(ex);
		}
	}

	public float queryPreviousClose(String symbol) {
		var url = "https://www.hkex.com.hk/eng/csm/ws/Result.asmx/GetData" //
				+ "?LangCode=en" //
				+ "&StockCode=" + HkexUtil.toStockCode(symbol) //
				+ "&mkt=hk" //
				+ "&location=priceMoveSearch" //
				+ "&ATypeSHEx=" //
				+ "&AType=" //
				+ "&PageNo=" //
				+ "&SearchMethod=1" //
				+ "&StockName=" //
				+ "&StockType=" //
				+ "&Ranking=" //
				+ "&FDD=" //
				+ "&FMM=" //
				+ "&FYYYY=" //
				+ "&TDD=" //
				+ "&TMM=" //
				+ "&TYYYY=";

		try (var is = HttpUtil.get(url).inputStream()) {
			return Read //
					.each(mapper.readTree(is)) //
					.flatMap(json_ -> json_.path("data")) //
					.filter(json_ -> String_.equals(json_.path("title").textValue(), "Stock price HKD")) //
					.flatMap(json_ -> json_.path("content")) //
					.filter(json_ -> String_.equals(json_.path(0).textValue(), "Previous<br>day close")) //
					.map(json_ -> Float.parseFloat(json_.path(1).textValue().split(" ")[0])) //
					.uniqueResult();
		} catch (IOException ex) {
			return Fail.t(ex);
		}
	}

	private List<Asset> queryCompanies_(int pageNo) {
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

		CompanySearch companySearch = mapper.convertValue(json, CompanySearch.class);
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
					.map(json_ -> Read.from(json_).map(JsonNode::asText).toList());

		var data1 = data0.collect();
		var lotSizeBySymbol = queryLotSizeBySymbol_(data1.map(this::toSymbol));
		return data1.map(datum -> toAsset(datum, lotSizeBySymbol)).toList();
	}

	private Map<String, Integer> queryLotSizeBySymbol_(Streamlet<String> symbols) {
		Source<Map<String, Integer>> fun = () -> symbols //
				.map2(symbol -> !delisted.contains(symbol) ? queryBoardLot_(symbol) : null) //
				.filterValue(boardLot -> boardLot != null) //
				.toMap();

		return SerializedStoreCache //
				.of(serialize.mapOfString(serialize.int_)) //
				.get(getClass().getSimpleName() + ".queryLotSizeBySymbol(" + symbols.collect(As.conc(",")) + ")", fun);
	}

	private int queryBoardLot_(String symbol) {
		if (String_.equals(symbol, "0700.HK"))
			return 100;
		else {
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

			CompanyInfo companyInfo = mapper.convertValue(json, CompanyInfo.class);

			var boardLotStr = Read //
					.each(companyInfo) //
					.flatMap(ci -> ci.data) //
					.concatMap(Data::tableEntries) //
					.filter(td -> String_.equals(td.get(0), "Board lot")) //
					.uniqueResult() //
					.get(1) //
					.replace(",", "");

			return Integer.parseInt(boardLotStr);
		}
	}

	private JsonNode query(String url) {
		JsonNode json;

		if (Boolean.TRUE)
			json = Singleton.me.storeCache.http(url).collect(To::inputStream).doRead(mapper::readTree);
		else {
			var execute = new Execute(new String[] { "curl", url, });
			json = Rethrow.ex(() -> mapper.readTree(execute.out));
		}

		return json;
	}

	private Asset toAsset(List<String> list, Map<String, Integer> lotSizeBySymbol) {
		var symbol = toSymbol(list);
		return Asset.of( //
				symbol, //
				list.get(2).trim(), //
				lotSizeBySymbol.get(symbol), //
				Integer.parseInt(list.get(3).substring(4).replace("\n", "").replace(",", "").trim()));
	}

	private String toSymbol(List<String> list) {
		return HkexUtil.toSymbol(list.get(1).replace("*", ""));
	}

}
