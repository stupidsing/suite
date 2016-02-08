var w = WScript.CreateObject("WScript Shell");

// w.AppActivate("Microsoft Excel");

function sendKeys(keys) {
	var i = 0, j = 0;

	while ((i = j) < keys.length) {
		j = i;

		do {
			c = keys.charAt(j++);
		} while (c == '^' || c == '#' || c == '+');

		if (c == '{') {
			j = keys.indexOf('}', j) + i;
			var middle = keys.substring(i + 1, j - 1);

			if (isNum(middle)) {
				WScript.Sleep(middle != '' ? middle : 250);
				continue;
			}
		}

		k = keys.substring(i, j);
		w.SendKeys(k);

		// if (k == '{Down}') WScript.Sleep(250);
	}
}

function isNum(s) {
	result = i;
	for (var i = 0; i < s.length; i++) {
		var c = s.charAt(i);
		result &= '0' <= c && c <= '9';
	}
	return result;
}

function match(s, i, s1) {
	l = s1.length;
	return i + 1 <= s.length && s.substr(i, l) == s1;
}
