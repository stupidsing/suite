package suite.trade;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import suite.node.util.Singleton;
import suite.os.Execute;
import suite.os.SerializedStoreCache;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Rethrow;
import suite.util.Serialize;
import suite.util.To;
import suite.util.Util;

public class Hkex {

	// .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	private static ObjectMapper mapper = new ObjectMapper();

	// https://www.hkex.com.hk/eng/csm/result.htm?location=companySearch&SearchMethod=2&mkt=hk&LangCode=en&StockType=MB&Ranking=ByMC&x=42&y=9
	// stock code, stock name, market capitalisation (million)
	private String lines = "" //
			+ "\n0700.HK|Tencent Holdings Ltd.|2134276" //
			+ "\n0941.HK|China Mobile Ltd.|1754749" //
			+ "\n0939.HK|China Construction Bank Corporation - H Shares|1521842" //
			+ "\n0005.HK|HSBC Holdings plc|1281059" //
			+ "\n1299.HK|AIA Group Ltd.|598097" //
			+ "\n1398.HK|Industrial and Commercial Bank of China Ltd. - H Shares|444386" //
			+ "\n2378.HK|Prudential plc|417660" //
			+ "\n0883.HK|CNOOC Ltd.|417007" //
			+ "\n0805.HK|Glencore plc|399229" //
			+ "\n0001.HK|Hutchison Holdings Ltd.|373230" //
			+ "\n2388.HK|BOC Hong Kong (Holdings) Ltd.|339386" //
			+ "\n0016.HK|Sun Hung Kai Properties Ltd.|332980" //
			+ "\n2318.HK|Ping An Insurance (Group) Co. of China, Ltd. - H Shares|328066" //
			+ "\n3988.HK|Bank of China Ltd. - H Shares|326127" //
			+ "\n0267.HK|CITIC Ltd.|322902" //
			+ "\n0011.HK|Hang Seng Bank Ltd.|303792" //
			+ "\n1928.HK|Sands China Ltd.|293805" //
			+ "\n0945.HK|Manulife Financial Corporation|271862" //
			+ "\n0066.HK|MTR Corporation Ltd.|259849" //
			+ "\n0762.HK|China Unicom (Hong Kong) Ltd.|251444" //
			+ "\n0688.HK|China Overseas Land & Investment Ltd.|243228" //
			+ "\n2888.HK|Standard Chartered PLC|238713" //
			+ "\n0388.HK|Hong Kong Exchanges and Clearing Ltd.|238008" //
			+ "\n3328.HK|Bank of Communications Co.Ltd. - H Shares|212172" //
			+ "\n0002.HK|CLP Holdings Ltd.|207043" //
			+ "\n0004.HK|Wharf (Holdings) Ltd., The|205157" //
			+ "\n1113.HK|Cheung Kong Property Holdings Ltd.|199320" //
			+ "\n0003.HK|Hong Kong and China Gas Co. Ltd., The|197623" //
			+ "\n0027.HK|Galaxy Entertainment Group Ltd.|184276" //
			+ "\n2628.HK|China Life Insurance Co. Ltd. - H Shares|178216" //
			+ "\n0012.HK|Henderson Land Development Co. Ltd.|175869" //
			+ "\n0566.HK|Hanergy Thin Film Power Group Ltd.|163669" //
			+ "\n1038.HK|Cheung Kong Infrastructure Holdings Ltd.|163149" //
			+ "\n0386.HK|China Petroleum & Chemical Corporation - H Shares|162265" //
			+ "\n2007.HK|Country Garden Holdings Co. Ltd.|149690" //
			+ "\n1109.HK|China Resources Land Ltd.|146936" //
			+ "\n1972.HK|Swire Properties Ltd.|145665" //
			+ "\n0006.HK|Power Assets Holdings Ltd.|142782" //
			+ "\n0020.HK|Wheelock and Co. Ltd.|127739" //
			+ "\n0857.HK|PetroChina Co. Ltd. - H Shares|121530" //
			+ "\n2018.HK|AAC Technologies Holdings Inc.|113222" //
			+ "\n1288.HK|Agricultural Bank of China Ltd. - H Shares|110660" //
			+ "\n0175.HK|Geely Automobile Holdings Ltd.|107777" //
			+ "\n0656.HK|Fosun International Ltd.|101370" //
			+ "\n3333.HK|China Evergrande Group|100450" //
			+ "\n0288.HK|WH Group Ltd.|99041" //
			+ "\n1658.HK|Postal Savings Bank of China Co., Ltd. - H Shares|95310" //
			+ "\n3968.HK|China Merchants Bank Co., Ltd. - H Shares|94573" //
			+ "\n0017.HK|New World Development Co. Ltd.|93677" //
			+ "\n0101.HK|Hang Lung Properties Ltd.|89232" //
			+ "\n0023.HK|Bank of East Asia, Ltd., The|88512" //
			+ "\n1913.HK|PRADA S.p.A.|85976" //
			+ "\n0083.HK|Sino Land Co. Ltd.|85651" //
			+ "\n1128.HK|Wynn Macau, Ltd.|83232" //
			+ "\n2799.HK|China Huarong Asset Management Co., Ltd. - H Shares|81893" //
			+ "\n2601.HK|China Pacific Insurance (Group) Co., Ltd. - H Shares|78819" //
			+ "\n0998.HK|China CITIC Bank Corporation Ltd. - H Shares|77090" //
			+ "\n6823.HK|HKT Trust and HKT Ltd. - SS|76020" //
			+ "\n0960.HK|Longfor Properties Co. Ltd.|75159" //
			+ "\n1929.HK|Chow Tai Fook Jewellery Group Ltd.|75000" //
			+ "\n0270.HK|Guangdong Investment Ltd.|72673" //
			+ "\n0019.HK|Swire Pacific Ltd. 'A'|71285" //
			+ "\n6808.HK|Sun Art Retail Group Ltd.|70689" //
			+ "\n1044.HK|Hengan International Group Co. Ltd.|69522" //
			+ "\n2313.HK|Shenzhou International Group Holdings Ltd.|68971" //
			+ "\n0607.HK|Fullshare Holdings Ltd.|68263" //
			+ "\n1114.HK|Brilliance China Automotive Holdings Ltd.|68240" //
			+ "\n0836.HK|China Resources Power Holdings Co. Ltd.|67897" //
			+ "\n0966.HK|China Taiping Insurance Holdings Co. Ltd.|67208" //
			+ "\n0151.HK|Want Want China Holdings Ltd.|67201" //
			+ "\n0384.HK|China Gas Holdings Ltd.|63626" //
			+ "\n3311.HK|China State Construction International Holdings Ltd.|63462" //
			+ "\n2638.HK|HK Electric Investments and HK Electric Investments Ltd. -SS|63356" //
			+ "\n0135.HK|Kunlun Energy Co. Ltd.|62400" //
			+ "\n1093.HK|CSPC Pharmaceutical Group Ltd.|62356" //
			+ "\n1193.HK|China Resources Gas Group Ltd.|62161" //
			+ "\n1088.HK|China Shenhua Energy Co. Ltd. - H Shares|62126" //
			+ "\n2382.HK|Sunny Optical Technology (Group) Co. Ltd.|61816" //
			+ "\n2319.HK|China Mengniu Dairy Co. Ltd.|61780" //
			+ "\n2282.HK|MGM China Holdings Ltd.|61408" //
			+ "\n3799.HK|Dali Foods Group Co. Ltd.|61076" //
			+ "\n0486.HK|United Company RUSAL Plc|60924" //
			+ "\n0144.HK|China Merchants Port Holdings Co. Ltd.|59342" //
			+ "\n0669.HK|Techtronic Industries Co. Ltd.|58459" //
			+ "\n0291.HK|China Resources Beer (Holdings) Co. Ltd.|58265" //
			+ "\n0992.HK|Lenovo Group Ltd.|57987" //
			+ "\n1988.HK|China Minsheng Banking Corp., Ltd. - H Shares|57618" //
			+ "\n3320.HK|China Resources Pharmaceutical Group Ltd.|56875" //
			+ "\n1357.HK|Meitu, Inc.|55490" //
			+ "\n2020.HK|ANTA Sports Products Ltd.|55054" //
			+ "\n0322.HK|Tingyi (Cayman Islands) Holding Corp.|54980" //
			+ "\n2328.HK|PICC Property and Casualty Co. Ltd. - H Shares|54918" //
			+ "\n0659.HK|NWS Holdings Ltd.|54774" //
			+ "\n0392.HK|Beijing Enterprises Holdings Ltd.|53452" //
			+ "\n0371.HK|Beijing Enterprises Water Group Ltd.|53400" //
			+ "\n0728.HK|China Telecom Corporation Ltd. - H Shares|52595" //
			+ "\n1378.HK|China Hongqiao Group Ltd.|51181" //
			+ "\n0551.HK|Yue Yuen Industrial (Holdings) Ltd.|50705" //
			+ "\n1169.HK|Haier Electronics Group Co., Ltd.|49699" //
			+ "\n1800.HK|China Communications Construction Co. Ltd. - H Shares|49234" //
			+ "\n2098.HK|Zall Group Ltd.|49000" //
			+ "\n2688.HK|ENN Energy Holdings Ltd.|48822" //
			+ "\n0257.HK|China Everbright International Ltd.|47337" //
			+ "\n1177.HK|Sino Biopharmaceutical Ltd.|47290" //
			+ "\n0010.HK|Hang Lung Group Ltd.|45274" //
			+ "\n0981.HK|Semiconductor Manufacturing International Corporation|45123" //
			+ "\n6837.HK|Haitong Securities Co., Ltd. - H Shares|44734" //
			+ "\n0293.HK|Cathay Pacific Airways Ltd.|44688" //
			+ "\n1099.HK|Sinopharm Group Co. Ltd. - H Shares|43359" //
			+ "\n1880.HK|Belle International Holdings Ltd.|42846" //
			+ "\n0522.HK|ASM Pacific Technology Ltd.|42661" //
			+ "\n0813.HK|Shimao Property Holdings Ltd.|42202" //
			+ "\n0069.HK|Shangri-La Asia Ltd.|41958" //
			+ "\n1359.HK|China Cinda Asset Management Co., Ltd. - H Shares|41110" //
			+ "\n1918.HK|Sunac China Holdings Ltd.|40932" //
			+ "\n0247.HK|Tsim Sha Tsui Properties Ltd.|40777" //
			+ "\n0087.HK|Swire Pacific Ltd. 'B'|40615" //
			+ "\n0683.HK|Kerry Properties Ltd.|39975" //
			+ "\n1211.HK|BYD Co. Ltd. - H Shares|39940" //
			+ "\n1910.HK|Samsonite International S.A.|39802" //
	;

	private Streamlet<Company> companies = Read //
			.from(lines.split("\n")) //
			.filter(line -> !line.isEmpty()) //
			.map(line -> line.split("\\|")) //
			.map(array -> new Company(array[0], array[1], Integer.parseInt(array[2]))) //
			.collect(As::streamlet);

	private Map<String, Company> companyByCode = Read.from(companies).toMap(company -> company.code);

	public class Company {
		public final String code;
		public final String name;
		public final int marketCap; // HKD million

		private Company(String code, String name, int marketCap) {
			this.code = code;
			this.name = name;
			this.marketCap = marketCap;
		}

		public String shortName() {
			return name.split(" ")[0];
		}

		public String toString() {
			return code + " " + name;
		}
	}

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
					return Arrays.asList(mapper.convertValue(table, Table.class));
			}
		}

		public int id;
		public String tabName;
		public String title;
		public List<Content> content;
		public String remark;

		private Streamlet<List<String>> tableEntries() {
			return Read.from(content) //
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

	public Company getCompany(String code) {
		return companyByCode.get(code);
	}

	public Streamlet<Company> getCompanies() {
		return companies;
	}

	public Streamlet<Company> queryCompanies() {
		return Read.each(queryCompanies(0), queryCompanies(1), queryCompanies(2)) //
				.flatMap(list -> list);
	}

	private List<Company> queryCompanies(int pageNo) {
		JsonNode json = query("" //
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

		if (Boolean.TRUE)
			return Read.each(companySearch) //
					.flatMap(cs -> cs.data) //
					.concatMap(Data::tableEntries) //
					.map(list -> new Company( //
							Util.right("0000" + list.get(1).replace("*", "").trim(), -4) + ".HK", //
							list.get(2).trim(), //
							Integer.parseInt(list.get(3).substring(4).replace("\n", "").replace(",", "").trim()))) //
					.toList();
		else
			return Read.each(json) //
					.flatMap(json_ -> json_.get("data")) //
					.flatMap(json_ -> json_.get("content")) //
					.flatMap(json_ -> json_.get("table")) //
					.flatMap(json_ -> json_.get("tr")) //
					.filter(json_ -> !json_.get("thead").asBoolean()) //
					.flatMap(json_ -> json_.get("td")) //
					.map(json_ -> Read.from(json_).map(JsonNode::asText).toList()) //
					.map(list -> new Company( //
							Util.right("0000" + list.get(1).replace("*", "").trim(), -4) + ".HK", //
							list.get(2).trim(), //
							Integer.parseInt(list.get(3).substring(4).replace("\n", "").replace(",", "").trim()))) //
					.toList();
	}

	public Map<String, Integer> queryLotSizeByStockCode(Streamlet<Company> companies) {
		return SerializedStoreCache //
				.of(Serialize.mapOfString(Serialize.int_)) //
				.get("lotSizeByStockCode",
						() -> companies //
								.map(stock -> stock.code) //
								.map2(stockCode -> queryBoardLot(stockCode)) //
								.toMap());
	}

	public int queryBoardLot(String stockCode0) {
		if (Util.stringEquals(stockCode0, "0700.HK"))
			return 100; // server return some unexpected errors, handle manually
		else {
			String stockCode = "" + Integer.parseInt(stockCode0.replace(".HK", ""));
			return queryBoardLot0(stockCode);
		}
	}

	private int queryBoardLot0(String stockCode) {
		JsonNode json = query("" //
				+ "https://www.hkex.com.hk/eng/csm/ws/Company.asmx/GetData" //
				+ "?location=companySearch" //
				+ "&SearchMethod=1" //
				+ "&LangCode=en" //
				+ "&StockCode=" + stockCode //
				+ "&StockName=" //
				+ "&mkt=hk" //
				+ "&x=" //
				+ "&y=");

		CompanyInfo companyInfo = mapper.convertValue(json, CompanyInfo.class);

		String boardLotStr = Read.each(companyInfo) //
				.flatMap(ci -> ci.data) //
				.concatMap(Data::tableEntries) //
				.filter(td -> Util.stringEquals(td.get(0), "Board lot")) //
				.uniqueResult() //
				.get(1) //
				.replace(",", "");

		return Integer.parseInt(boardLotStr);
	}

	private JsonNode query(String url) {
		JsonNode json;

		if (Boolean.TRUE)
			try (InputStream is = Singleton.get().getStoreCache().http(url).collect(To::inputStream)) {
				json = Rethrow.ex(() -> mapper.readTree(is));
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		else {
			Execute execute = new Execute(new String[] { "curl", url, });
			json = Rethrow.ex(() -> mapper.readTree(execute.out));
		}

		return json;
	}

}
