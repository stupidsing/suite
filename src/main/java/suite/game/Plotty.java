package suite.game;

import java.io.File;

import suite.cfg.Defaults;
import suite.os.Execute;
import suite.os.FileUtil;
import suite.primitive.Floats_;
import suite.primitive.adt.pair.FltFltPair;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Util;

public class Plotty {

	private String[] browsers = { "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe", "/usr/bin/chromium", };

	public boolean plot(Streamlet<float[]> xyts) {
		var data = xyts //
				.map(xyt -> Floats_.of(xyt).index().map((y, x) -> FltFltPair.of(x, y)).collect(this::xyt) + ",") //
				.collect(As::joined);

		var html = "" //
				+ "<head><script src='https://cdn.plot.ly/plotly-latest.min.js'></script></head>" //
				+ "<body><div id='plot'></div></body>" //
				+ "<script>" //
				+ "Plotly.newPlot('plot', [" + data + "], {" //
				+ "	yaxis: { rangemode: 'tozero', zeroline: true, }" //
				+ "});" //
				+ "</script>";

		var file = Defaults.tmp("plot$" + Util.temp() + ".html");

		FileUtil.out(file).writeAndClose(html);

		Read.from(browsers).filter(b -> new File(b).exists())
				.forEach(browser -> Execute.shell("'" + browser + "' --incognito '" + file + "'"));

		return true;
	}

	private String xyt(Outlet<FltFltPair> xys0) {
		var xys1 = Read.from(xys0.toList());
		var xs = xys1.map(xy -> xy.t0 + ",").collect(As::joined);
		var ys = xys1.map(xy -> xy.t1 + ",").collect(As::joined);
		return "{ x: [" + xs + "], y: [" + ys + "], type: 'scatter', }";
	}

}
