package suite.text;

import static java.util.Map.entry;
import static suite.util.Friends.fail;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import suite.streamlet.Read;

public class ParseDate {

	private Map<String, Integer> monthValueByName = Map.ofEntries( //
			entry("JAN", 1), //
			entry("FEB", 2), //
			entry("MAR", 3), //
			entry("APR", 4), //
			entry("MAY", 5), //
			entry("JUN", 6), //
			entry("JUL", 7), //
			entry("AUG", 8), //
			entry("SEP", 9), //
			entry("OCT", 10), //
			entry("NOV", 11), //
			entry("DEC", 12));

	private Map<String, Integer> weekDayByName = Map.ofEntries( //
			entry("MON", 1), //
			entry("TUE", 2), //
			entry("WED", 3), //
			entry("THU", 4), //
			entry("FRI", 5), //
			entry("SAT", 6), //
			entry("SUN", 7));

	public Instant parse(String s) {
		var zdt = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC.normalized());

		return parse(s.toUpperCase(), new Object[] { //
				zdt.getYear(), //
				zdt.getMonthValue(), // 1 to 12
				zdt.getDayOfMonth(), // 1 to 31
				0, // zdt.getHour(), //
				0, // zdt.getMinute(), //
				0, // zdt.getSecond(), //
				0, // zdt.getNano(), //
				zdt.getOffset(), }, //
				-1);
	}

	public Instant parse(String s, Object[] ymdhmsnt, int lf) {
		char ch0;

		if (s.isEmpty())
			return ZonedDateTime.of( //
					(Integer) ymdhmsnt[0], (Integer) ymdhmsnt[1], (Integer) ymdhmsnt[2], //
					(Integer) ymdhmsnt[3], (Integer) ymdhmsnt[4], (Integer) ymdhmsnt[5], //
					(Integer) ymdhmsnt[6], //
					(ZoneOffset) ymdhmsnt[7]).toInstant();
		else
			ch0 = s.charAt(0);

		Match m;
		Integer i;

		if ((m = match("## $$$ ####", s)) != null && (i = toMonth(m.m[1])) != null) {
			ymdhmsnt[0] = Integer.parseInt(m.m[2]);
			ymdhmsnt[1] = i;
			ymdhmsnt[lf = 2] = Integer.parseInt(m.m[0]);
			return parse(m.tail, ymdhmsnt, lf);
		} else if ((m = match("####-##-##", s)) != null) {
			ymdhmsnt[0] = Integer.parseInt(m.m[0]);
			ymdhmsnt[1] = Integer.parseInt(m.m[1]);
			ymdhmsnt[lf = 2] = Integer.parseInt(m.m[2]);
			return parse(m.tail, ymdhmsnt, lf);
		} else if ((m = match("##:##:##.###", s)) != null) {
			ymdhmsnt[3] = Integer.parseInt(m.m[0]);
			ymdhmsnt[4] = Integer.parseInt(m.m[1]);
			ymdhmsnt[5] = Integer.parseInt(m.m[2]);
			ymdhmsnt[lf = 6] = Integer.parseInt(m.m[3]) * 1000000;
			return parse(m.tail, ymdhmsnt, lf);
		} else if ((m = match("##:##:##", s)) != null) {
			ymdhmsnt[3] = Integer.parseInt(m.m[0]);
			ymdhmsnt[4] = Integer.parseInt(m.m[1]);
			ymdhmsnt[5] = Integer.parseInt(m.m[2]);
			ymdhmsnt[lf = 6] = 0;
			return parse(m.tail, ymdhmsnt, lf);
		} else if (((m = match("#:##$$", s)) != null || (m = match("#:## $$", s)) != null //
				|| (m = match("##:##$$", s)) != null || (m = match("##:## $$", s)) != null) //
				&& Set.of("AM", "NN", "PM").contains(m.m[2])) {
			var h = Integer.parseInt(m.m[0]);
			if (m.m[2].equals("AM"))
				h = h != 12 ? h : 0;
			else if (m.m[2].equals("NN"))
				h = h != 12 ? 12 : 12;
			else if (m.m[2].equals("PM"))
				h = 12 <= h ? h : h + 12;
			ymdhmsnt[3] = h;
			ymdhmsnt[4] = Integer.parseInt(m.m[1]);
			ymdhmsnt[5] = 0;
			ymdhmsnt[lf = 6] = 0;
			return parse(m.tail, ymdhmsnt, lf);
		} else if ((m = match("##:##", s)) != null) {
			ymdhmsnt[3] = Integer.parseInt(m.m[0]);
			ymdhmsnt[4] = Integer.parseInt(m.m[1]);
			ymdhmsnt[5] = 0;
			ymdhmsnt[lf = 6] = 0;
			return parse(m.tail, ymdhmsnt, lf);
		} else if ((m = match("GMT-##", s)) != null || (m = match("GMT-#", s)) != null) {
			ymdhmsnt[lf = 7] = ZoneOffset.ofHours(-Integer.parseInt(m.m[0]));
			return parse(m.tail, ymdhmsnt, lf);
		} else if ((m = match("GMT+##", s)) != null || (m = match("GMT+#", s)) != null) {
			ymdhmsnt[lf = 7] = ZoneOffset.ofHours(Integer.parseInt(m.m[0]));
			return parse(m.tail, ymdhmsnt, lf);
		}

		var p = 0;

		if (isId(ch0))
			while (p < s.length() && isId(s.charAt(p)))
				p++;
		else if (Character.isDigit(ch0))
			while (p < s.length() && Character.isDigit(s.charAt(p)))
				p++;
		else if (ch0 == ' ' || ch0 == ',')
			p = 1;
		else
			p = 0;

		var word = s.substring(0, p);
		var cont = s.substring(p);

		if (word.equals(" ") || word.equals(","))
			;
		else if ((i = toMonth(word)) != null)
			ymdhmsnt[lf = 1] = i;
		else if (toWeekDay(word) != null)
			;

		else if (word.startsWith("T"))
			lf = 2;
		else if (Character.isDigit(ch0)) {
			i = Integer.parseInt(s.substring(0, p));
			if (1900 <= i && i < 10000)
				ymdhmsnt[lf = 0] = i;
			else if (lf != -1)
				ymdhmsnt[lf + 1] = i;
			else
				fail("ambiguous date");
		} else if (word.startsWith("UTC") || word.equals("Z"))
			ymdhmsnt[lf = 7] = ZoneOffset.UTC;
		else if (word.contains("/"))
			ymdhmsnt[lf = 7] = ZoneId.of(word);
		else
			fail("unknown word " + word);

		return parse(cont, ymdhmsnt, lf);
	}

	private class Match {
		private String[] m;
		private String tail;
	}

	private Match match(String pattern, String s) {
		var b = pattern.length() <= s.length();
		var list = new ArrayList<String>();
		var p0 = 0;
		var pc0 = (char) 0;
		var m = new Match();

		if (b) {
			for (var p = 0; p < pattern.length(); p++) {
				var pc = pattern.charAt(p);
				var sc = s.charAt(p);

				if (pc != pc0) {
					if (pc0 == '$' || pc0 == '#')
						list.add(s.substring(p0, p));
					p0 = p;
				}

				if (pc == '$')
					b &= Character.isAlphabetic(sc);
				else if (pc == '#')
					b &= Character.isDigit(sc);
				else
					b &= pc == sc;

				pc0 = pc;
			}

			if (pc0 == '$' || pc0 == '#')
				list.add(s.substring(p0, pattern.length()));

			m.m = Read.from(list).filter(w -> !w.isEmpty()).toArray(String.class);
			m.tail = s.substring(pattern.length());
		}

		return b ? m : null;
	}

	private Integer toMonth(String word) {
		return 3 <= word.length() ? monthValueByName.get(word.substring(0, 3)) : null;
	}

	private Integer toWeekDay(String word) {
		return 3 <= word.length() ? weekDayByName.get(word.substring(0, 3)) : null;
	}

	private boolean isId(char ch0) {
		return Character.isAlphabetic(ch0) || ch0 == '/' || ch0 == '_';
	}

}
