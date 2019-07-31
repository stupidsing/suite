package suite.trade.data;

import static primal.statics.Fail.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import primal.adt.Fixie;
import primal.adt.Fixie_.Fixie3;
import suite.node.util.Singleton;
import suite.streamlet.As;

public class Google {

	public Fixie3<Map<String, String>, String, List<String[]>> historical(String symbol) {
		var url = "" //
				+ "http://finance.google.com/finance/getprices" //
				+ "?q=" + fromSymbol(symbol) //
				+ "&x=HKG" //
				+ "&i=86400" //
				+ "&p=1Y" //
				+ "&f=d,c,h,l,o,v" //
		// + "&ts=" //
		;

		var lines = Singleton.me.storeCache //
				.http(url) //
				.collect(As::lines) //
				.toArray(String.class);

		var properties = new HashMap<String, String>();
		String[] array;
		var i = 0;

		properties.put("EXCHANGE", lines[i++]);

		while (i < lines.length && 1 < (array = lines[i].split("=")).length) {
			properties.put(array[0], array[1]);
			i++;
		}

		var header = lines[i++];
		var data = new ArrayList<String[]>();

		while (i < lines.length)
			data.add(lines[i++].split(","));

		return Fixie.of(properties, header, data);
	}

	private String fromSymbol(String symbol) {
		if (symbol.startsWith("^"))
			return symbol.substring(1);
		else if (symbol.endsWith(".HK"))
			return symbol.substring(0, 4);
		else
			return fail();
	}

}
