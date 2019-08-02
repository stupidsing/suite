package suite.game;

import java.io.File;

import primal.Verbs.Get;
import primal.Verbs.WriteFile;
import primal.primitive.adt.pair.FltFltPair;
import primal.puller.Puller;
import suite.cfg.Defaults;
import suite.os.Execute;
import suite.primitive.Floats_;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class Plotty {

	private String[] browsers = { "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe", "/usr/bin/chromium-browser", };

	public boolean plot(Streamlet<float[]> xyts) {
		var data = xyts //
				.map(xyt -> Floats_.of(xyt).index().map((y, x) -> FltFltPair.of(x, y)).collect(this::xyt) + ",") //
				.collect(As::joined);

		var file = Defaults.tmp("plot$" + Get.temp() + ".html");

		WriteFile.to(file).writeAndClose("" //
				+ "<head><script src='https://cdn.plot.ly/plotly-latest.min.js'></script></head>" //
				+ "<body><div id='plot'></div></body>" //
				+ "<script>" //
				+ "Plotly.newPlot('plot', [" + data + "], {" //
				+ "	yaxis: { rangemode: 'tozero', zeroline: true, }" //
				+ "});" //
				+ "</script>");

		Read.from(browsers).filter(b -> new File(b).exists())
				.forEach(browser -> Execute.shell("'" + browser + "' --incognito '" + file + "'"));

		return true;
	}

	private String xyt(Puller<FltFltPair> xys0) {
		var xys1 = Read.from(xys0.toList());
		var xs = xys1.map(xy -> xy.t0 + ",").collect(As::joined);
		var ys = xys1.map(xy -> xy.t1 + ",").collect(As::joined);
		return "{ x: [" + xs + "], y: [" + ys + "], type: 'scatter', }";
	}

}
