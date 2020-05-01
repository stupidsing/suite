package suite.game;

import primal.MoreVerbs.Read;
import primal.Nouns.Tmp;
import primal.Verbs.Get;
import primal.Verbs.WriteFile;
import primal.primitive.FltMoreVerbs.ReadFlt;
import primal.primitive.adt.pair.FltFltPair;
import primal.puller.Puller;
import primal.streamlet.Streamlet;
import suite.os.Execute;

import java.io.File;

public class Plotty {

	private String[] browsers = { "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe", "/usr/bin/chromium-browser", };

	public boolean plot(Streamlet<float[]> xyts) {
		var data = xyts
				.map(xyt -> ReadFlt.from(xyt).index().map((y, x) -> FltFltPair.of(x, y)).collect(this::xyt) + ",")
				.toJoinedString();

		var file = Tmp.path("plot$" + Get.temp() + ".html");

		WriteFile.to(file).writeAndClose(""
				+ "<head><script src='https://cdn.plot.ly/plotly-latest.min.js'></script></head>"
				+ "<body><div id='plot'></div></body>"
				+ "<script>"
				+ "Plotly.newPlot('plot', [" + data + "], {"
				+ "	yaxis: { rangemode: 'tozero', zeroline: true, }"
				+ "});"
				+ "</script>");

		Read.from(browsers).filter(b -> new File(b).exists())
				.forEach(browser -> Execute.shell("'" + browser + "' --incognito '" + file + "'"));

		return true;
	}

	private String xyt(Puller<FltFltPair> xys0) {
		var xys1 = Read.from(xys0.toList());
		var xs = xys1.map(xy -> xy.t0 + ",").toJoinedString();
		var ys = xys1.map(xy -> xy.t1 + ",").toJoinedString();
		return "{ x: [" + xs + "], y: [" + ys + "], type: 'scatter', }";
	}

}
