package suite.trade.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import suite.node.util.Singleton;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Time;
import suite.util.String_;

public class HongKongGovernment {

	public List<Time> queryPublicHolidays() {
		DateTimeFormatter yyyymmdd = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ENGLISH);

		return Singleton.me.storeCache //
				.http("http://www.1823.gov.hk/common/ical/gc/en.ics") //
				.collect(As::lines) //
				.map(line -> line.split(":")) //
				.filter(array -> 2 <= array.length) //
				.split(array -> String_.equals(array[0], "BEGIN") && String_.equals(array[1], "VEVENT")) //
				.map(arrays -> Read.from(arrays) //
						.map2(array -> array[0], array -> array[1]) //
						.toMap()) //
				.map2(map -> map.get("DTSTART;VALUE=DATE"), map -> map.get("SUMMARY")) //
				.filterKey(s -> s != null) //
				.keys() //
				.map(s -> Time.of(LocalDate.parse(s, yyyymmdd).atStartOfDay())) //
				.toList();
	}

}
