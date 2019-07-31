package suite.http;

import static primal.statics.Fail.fail;

import suite.persistent.PerList;
import suite.persistent.PerMap;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;

public class HttpHeader {

	private PerMap<String, PerList<String>> map;

	public HttpHeader() {
		this(PerMap.empty());
	}

	public HttpHeader(PerMap<String, PerList<String>> map) {
		this.map = map;
	}

	public String get(String key) {
		var list = map.get(key);
		if (list != null)
			return list.tail.isEmpty() ? list.head : fail();
		else
			return null;
	}

	public HttpHeader put(String key, String value) {
		var list0 = map.get(key);
		var list1 = list0 != null ? list0 : PerList.<String> end();
		return new HttpHeader(map.replace(key, PerList.cons(value, list1)));
	}

	public HttpHeader remove(String key) {
		return new HttpHeader(map.remove(key));
	}

	public Streamlet2<String, String> streamlet() {
		return Read.from2(map).concatMapValue(Read::from);
	}

}
