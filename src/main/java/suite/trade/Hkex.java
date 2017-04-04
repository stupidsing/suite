package suite.trade;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import suite.http.HttpUtil;
import suite.os.Execute;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.To;
import suite.util.Util;

public class Hkex {

	private ObjectMapper mapper = new ObjectMapper();

	// https://www.hkex.com.hk/eng/csm/result.htm?location=companySearch&SearchMethod=2&mkt=hk&LangCode=en&StockType=MB&Ranking=ByMC&x=42&y=9
	// stock code, stock name, market capitalisation (million)
	private String lines = "" //
			+ "\n0700|Tencent Holdings Ltd.|2134276" //
			+ "\n0941|China Mobile Ltd.|1754749" //
			+ "\n0939|China Construction Bank Corporation - H Shares|1521842" //
			+ "\n0005|HSBC Holdings plc|1281059" //
			+ "\n1299|AIA Group Ltd.|598097" //
			+ "\n1398|Industrial and Commercial Bank of China Ltd. - H Shares|444386" //
			+ "\n2378|Prudential plc|417660" //
			+ "\n0883|CNOOC Ltd.|417007" //
			+ "\n0805|Glencore plc|399229" //
			+ "\n0001|Hutchison Holdings Ltd.|373230" //
			+ "\n2388|BOC Hong Kong (Holdings) Ltd.|339386" //
			+ "\n0016|Sun Hung Kai Properties Ltd.|332980" //
			+ "\n2318|Ping An Insurance (Group) Co. of ChinaLtd. - H Shares|328066" //
			+ "\n3988|Bank of China Ltd. - H Shares|326127" //
			+ "\n0267|CITIC Ltd.|322902" //
			+ "\n0011|Hang Seng Bank Ltd.|303792" //
			+ "\n1928|Sands China Ltd.|293805" //
			+ "\n0945|Manulife Financial Corporation|271862" //
			+ "\n0066|MTR Corporation Ltd.|259849" //
			+ "\n0762|China Unicom (Hong Kong) Ltd.|251444" //
			+ "\n0688|China Overseas Land & Investment Ltd.|243228" //
			+ "\n2888|Standard Chartered PLC|238713" //
			+ "\n0388|Hong Kong Exchanges and Clearing Ltd.|238008" //
			+ "\n3328|Bank of Communications Co.Ltd. - H Shares|212172" //
			+ "\n0002|CLP Holdings Ltd.|207043" //
			+ "\n0004|Wharf (Holdings) Ltd., The|205157" //
			+ "\n1113|Cheung Kong Property Holdings Ltd.|199320" //
			+ "\n0003|Hong Kong and China Gas Co. Ltd., The|197623" //
			+ "\n0027|Galaxy Entertainment Group Ltd.|184276" //
			+ "\n2628|China Life Insurance Co. Ltd. - H Shares|178216" //
			+ "\n0012|Henderson Land Development Co. Ltd.|175869" //
			+ "\n0566|Hanergy Thin Film Power Group Ltd.|163669" //
			+ "\n1038|Cheung Kong Infrastructure Holdings Ltd.|163149" //
			+ "\n0386|China Petroleum & Chemical Corporation - H Shares|162265" //
			+ "\n2007|Country Garden Holdings Co. Ltd.|149690" //
			+ "\n1109|China Resources Land Ltd.|146936" //
			+ "\n1972|Swire Properties Ltd.|145665" //
			+ "\n0006|Power Assets Holdings Ltd.|142782" //
			+ "\n0020|Wheelock and Co. Ltd.|127739" //
			+ "\n0857|PetroChina Co. Ltd. - H Shares|121530" //
			+ "\n2018|AAC Technologies Holdings Inc.|113222" //
			+ "\n1288|Agricultural Bank of China Ltd. - H Shares|110660" //
			+ "\n0175|Geely Automobile Holdings Ltd.|107777" //
			+ "\n0656|Fosun International Ltd.|101370" //
			+ "\n3333|China Evergrande Group|100450" //
			+ "\n0288|WH Group Ltd.|99041" //
			+ "\n1658|Postal Savings Bank of China Co., Ltd. - H Shares|95310" //
			+ "\n3968|China Merchants Bank Co., Ltd. - H Shares|94573" //
			+ "\n0017|New World Development Co. Ltd.|93677" //
			+ "\n0101|Hang Lung Properties Ltd.|89232" //
			+ "\n0023|Bank of East Asia, Ltd., The|88512" //
			+ "\n1913|PRADA S.p.A.|85976" //
			+ "\n0083|Sino Land Co. Ltd.|85651" //
			+ "\n1128|Wynn Macau, Ltd.|83232" //
			+ "\n2799|China Huarong Asset Management Co., Ltd. - H Shares|81893" //
			+ "\n2601|China Pacific Insurance (Group) Co., Ltd. - H Shares|78819" //
			+ "\n0998|China CITIC Bank Corporation Ltd. - H Shares|77090" //
			+ "\n6823|HKT Trust and HKT Ltd. - SS|76020" //
			+ "\n0960|Longfor Properties Co. Ltd.|75159" //
			+ "\n1929|Chow Tai Fook Jewellery Group Ltd.|75000" //
			+ "\n0270|Guangdong Investment Ltd.|72673" //
			+ "\n0019|Swire Pacific Ltd. 'A'|71285" //
			+ "\n6808|Sun Art Retail Group Ltd.|70689" //
			+ "\n1044|Hengan International Group Co. Ltd.|69522" //
			+ "\n2313|Shenzhou International Group Holdings Ltd.|68971" //
			+ "\n0607|Fullshare Holdings Ltd.|68263" //
			+ "\n1114|Brilliance China Automotive Holdings Ltd.|68240" //
			+ "\n0836|China Resources Power Holdings Co. Ltd.|67897" //
			+ "\n0966|China Taiping Insurance Holdings Co. Ltd.|67208" //
			+ "\n0151|Want Want China Holdings Ltd.|67201" //
			+ "\n0384|China Gas Holdings Ltd.|63626" //
			+ "\n3311|China State Construction International Holdings Ltd.|63462" //
			+ "\n2638|HK Electric Investments and HK Electric Investments Ltd. -SS|63356" //
			+ "\n0135|Kunlun Energy Co. Ltd.|62400" //
			+ "\n1093|CSPC Pharmaceutical Group Ltd.|62356" //
			+ "\n1193|China Resources Gas Group Ltd.|62161" //
			+ "\n1088|China Shenhua Energy Co. Ltd. - H Shares|62126" //
			+ "\n2382|Sunny Optical Technology (Group) Co. Ltd.|61816" //
			+ "\n2319|China Mengniu Dairy Co. Ltd.|61780" //
			+ "\n2282|MGM China Holdings Ltd.|61408" //
			+ "\n3799|Dali Foods Group Co. Ltd.|61076" //
			+ "\n0486|United Company RUSAL Plc|60924" //
			+ "\n0144|China Merchants Port Holdings Co. Ltd.|59342" //
			+ "\n0669|Techtronic Industries Co. Ltd.|58459" //
			+ "\n0291|China Resources Beer (Holdings) Co. Ltd.|58265" //
			+ "\n0992|Lenovo Group Ltd.|57987" //
			+ "\n1988|China Minsheng Banking Corp., Ltd. - H Shares|57618" //
			+ "\n3320|China Resources Pharmaceutical Group Ltd.|56875" //
			+ "\n1357|Meitu, Inc.|55490" //
			+ "\n2020|ANTA Sports Products Ltd.|55054" //
			+ "\n0322|Tingyi (Cayman Islands) Holding Corp.|54980" //
			+ "\n2328|PICC Property and Casualty Co. Ltd. - H Shares|54918" //
			+ "\n0659|NWS Holdings Ltd.|54774" //
			+ "\n0392|Beijing Enterprises Holdings Ltd.|53452" //
			+ "\n0371|Beijing Enterprises Water Group Ltd.|53400" //
			+ "\n0728|China Telecom Corporation Ltd. - H Shares|52595" //
			+ "\n1378|China Hongqiao Group Ltd.|51181" //
			+ "\n0551|Yue Yuen Industrial (Holdings) Ltd.|50705" //
			+ "\n1169|Haier Electronics Group Co., Ltd.|49699" //
			+ "\n1800|China Communications Construction Co. Ltd. - H Shares|49234" //
			+ "\n2098|Zall Group Ltd.|49000" //
			+ "\n2688|ENN Energy Holdings Ltd.|48822" //
			+ "\n0257|China Everbright International Ltd.|47337" //
			+ "\n1177|Sino Biopharmaceutical Ltd.|47290" //
			+ "\n0010|Hang Lung Group Ltd.|45274" //
			+ "\n0981|Semiconductor Manufacturing International Corporation|45123" //
			+ "\n6837|Haitong Securities Co., Ltd. - H Shares|44734" //
			+ "\n0293|Cathay Pacific Airways Ltd.|44688" //
			+ "\n1099|Sinopharm Group Co. Ltd. - H Shares|43359" //
			+ "\n1880|Belle International Holdings Ltd.|42846" //
			+ "\n0522|ASM Pacific Technology Ltd.|42661" //
			+ "\n0813|Shimao Property Holdings Ltd.|42202" //
			+ "\n0069|Shangri-La Asia Ltd.|41958" //
			+ "\n1359|China Cinda Asset Management Co., Ltd. - H Shares|41110" //
			+ "\n1918|Sunac China Holdings Ltd.|40932" //
			+ "\n0247|Tsim Sha Tsui Properties Ltd.|40777" //
			+ "\n0087|Swire Pacific Ltd. 'B'|40615" //
			+ "\n0683|Kerry Properties Ltd.|39975" //
			+ "\n1211|BYD Co. Ltd. - H Shares|39940" //
			+ "\n1910|Samsonite International S.A.|39802" //
	;

	public List<Company> companies = Read //
			.from(lines.split("\n")) //
			.filter(line -> !line.isEmpty()) //
			.map(line -> line.split("\\|")) //
			.map(array -> new Company(array[0], array[1], Integer.parseInt(array[2]))) //
			.toList();

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
	}

	public static class CompanySearch {
		public static class Data {
			public static class Content {
				public static class Table {
					public static class Tr {
						public boolean thead;
						public List<List<String>> td;
						public String link;
					}

					public String classname;
					public List<String> colAlign;
					public List<Tr> tr;
				}

				public int style;
				public String classname;
				public List<Table> table;
			}

			public int id;
			public List<Content> content;
			public String remark;
		}

		public String IndexName;
		public List<Data> data;
		public String PageNo;
		public int PageSize;
		public int TotalCount;
		public String LastUpdateDate;
	}

	public Company getCompany(String code) {
		return companyByCode.get(code);
	}

	public List<Company> list() {
		return list(0);
	}

	public List<Company> list(int pageNo) {
		String url = "https://www.hkex.com.hk/eng/csm/ws/Result.asmx/GetData" //
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
				+ "&TYYYY=";

		JsonNode json;

		if (Boolean.TRUE) {
			Execute execute = new Execute(new String[] { "curl", url, });
			json = Rethrow.ex(() -> mapper.readTree(execute.out));
		} else {
			String referer = "https://www.hkex.com.hk/eng/csm/result.htm?location=companySearch";
			Map<String, String> headers = To.map("Referer", referer);
			InputStream is = HttpUtil.http("GET", Rethrow.ex(() -> new URL(url)), headers).out.collect(To::inputStream);
			json = Rethrow.ex(() -> mapper.readTree(is));
		}

		CompanySearch companySearch = mapper.convertValue(json, CompanySearch.class);

		return Read.from(companySearch.data) //
				.concatMap(data -> Read.from(data.content)) //
				.concatMap(content -> Read.from(content.table)) //
				.concatMap(table -> Read.from(table.tr)) //
				.filter(tr -> !tr.thead) //
				.concatMap(tr -> Read.from(tr.td)) //
				.map(list -> new Company( //
						Util.right("0000" + list.get(1).replace("*", "").trim(), -4), //
						list.get(2).trim(), //
						Integer.parseInt(list.get(3).substring(4).replace("\n", "").replace(",", "").trim()))) //
				.toList();
	}

}
