package suite.trade;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import suite.adt.Fixie;
import suite.adt.Fixie.D_;
import suite.http.HttpUtil;
import suite.os.Execute;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.To;

public class Hkex {

	private ObjectMapper mapper = new ObjectMapper();

	// https://www.hkex.com.hk/eng/csm/result.htm?location=companySearch&SearchMethod=2&mkt=hk&LangCode=en&StockType=MB&Ranking=ByMC&x=42&y=9
	// stock code, stock name, market capitalisation (million)
	public List<Fixie<String, String, Integer, D_, D_, D_, D_, D_, D_, D_>> hkex = Arrays.asList( //
			Fixie.of("0700", "Tencent Holdings Ltd.", 2134276), //
			Fixie.of("0941", "China Mobile Ltd.", 1754749), //
			Fixie.of("0939", "China Construction Bank Corporation - H Shares", 1521842), //
			Fixie.of("0005", "HSBC Holdings plc", 1281059), //
			Fixie.of("1299", "AIA Group Ltd.", 598097), //
			Fixie.of("1398", "Industrial and Commercial Bank of China Ltd. - H Shares", 444386), //
			Fixie.of("2378", "Prudential plc", 417660), //
			Fixie.of("0883", "CNOOC Ltd.", 417007), //
			Fixie.of("0805", "Glencore plc", 399229), //
			Fixie.of("0001", "Hutchison Holdings Ltd.", 373230), //
			Fixie.of("2388", "BOC Hong Kong (Holdings) Ltd.", 339386), //
			Fixie.of("0016", "Sun Hung Kai Properties Ltd.", 332980), //
			Fixie.of("2318", "Ping An Insurance (Group) Co. of ChinaLtd. - H Shares", 328066), //
			Fixie.of("3988", "Bank of China Ltd. - H Shares", 326127), //
			Fixie.of("0267", "CITIC Ltd.", 322902), //
			Fixie.of("0011", "Hang Seng Bank Ltd.", 303792), //
			Fixie.of("1928", "Sands China Ltd.", 293805), //
			Fixie.of("0945", "Manulife Financial Corporation", 271862), //
			Fixie.of("0066", "MTR Corporation Ltd.", 259849), //
			Fixie.of("0762", "China Unicom (Hong Kong) Ltd.", 251444), //
			Fixie.of("0688", "China Overseas Land & Investment Ltd.", 243228), //
			Fixie.of("2888", "Standard Chartered PLC", 238713), //
			Fixie.of("0388", "Hong Kong Exchanges and Clearing Ltd.", 238008), //
			Fixie.of("3328", "Bank of Communications Co.Ltd. - H Shares", 212172), //
			Fixie.of("0002", "CLP Holdings Ltd.", 207043), //
			Fixie.of("0004", "Wharf (Holdings) Ltd., The", 205157), //
			Fixie.of("1113", "Cheung Kong Property Holdings Ltd.", 199320), //
			Fixie.of("0003", "Hong Kong and China Gas Co. Ltd., The", 197623), //
			Fixie.of("0027", "Galaxy Entertainment Group Ltd.", 184276), //
			Fixie.of("2628", "China Life Insurance Co. Ltd. - H Shares", 178216), //
			Fixie.of("0012", "Henderson Land Development Co. Ltd.", 175869), //
			Fixie.of("0566", "Hanergy Thin Film Power Group Ltd.", 163669), //
			Fixie.of("1038", "Cheung Kong Infrastructure Holdings Ltd.", 163149), //
			Fixie.of("0386", "China Petroleum & Chemical Corporation - H Shares", 162265), //
			Fixie.of("2007", "Country Garden Holdings Co. Ltd.", 149690), //
			Fixie.of("1109", "China Resources Land Ltd.", 146936), //
			Fixie.of("1972", "Swire Properties Ltd.", 145665), //
			Fixie.of("0006", "Power Assets Holdings Ltd.", 142782), //
			Fixie.of("0020", "Wheelock and Co. Ltd.", 127739), //
			Fixie.of("0857", "PetroChina Co. Ltd. - H Shares", 121530));

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

	public List<Fixie<String, String, Integer, D_, D_, D_, D_, D_, D_, D_>> list() {
		String url = "https://www.hkex.com.hk/eng/csm/ws/Result.asmx/GetData" //
				+ "?location=companySearch" //
				+ "&SearchMethod=2" //
				+ "&LangCode=en" //
				+ "&StockCode=" //
				+ "&StockName=" //
				+ "&Ranking=ByMC" //
				+ "&StockType=MB" //
				+ "&mkt=hk" //
				+ "&PageNo=1" //
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
				.map(list -> Fixie.of( //
						list.get(1).trim(), //
						list.get(2).trim(), //
						Integer.parseInt(list.get(3).substring(4).replace("\n", "").replace(",", "").trim()))) //
				.toList();
	}

}
