package suite.game;

import java.io.File;

import suite.cfg.Defaults;
import suite.os.Execute;
import suite.os.FileUtil;
import suite.primitive.Floats_;
import suite.primitive.adt.pair.FltFltPair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.RunUtil;

public class PlottyMain {

	private String[] browsers = { "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe", "/usr/bin/chromium", };

	public static void main(String[] args) {
		RunUtil.run(() -> new PlottyMain().run());
	}

	private boolean run() {
		var d0 = new float[] { 10f, 15f, 13f, 17f, };
		var d1 = new float[] { 16f, 5f, 11f, 9f, };
		var xyts = Read.each(d0, d1);

		var html = "" //
				+ "<head><script src='https://cdn.plot.ly/plotly-latest.min.js'></script></head>" //
				+ "<body><div id='plot'></div></body>" //
				+ "<script>" //
				+ "Plotly.newPlot('plot', [" + xyts.map(xyt -> xyt(xyt) + ",").collect(As::joined) + "], {" //
				+ "	yaxis: { rangemode: 'tozero', zeroline: true, }" //
				+ "});" //
				+ "</script>";

		var file = Defaults.tmp("plot.html");

		FileUtil.out(file).writeAndClose(html);

		Read.from(browsers).filter(b -> new File(b).exists())
				.forEach(browser -> Execute.shell("'" + browser + "' --incognito '" + file + "'"));

		return true;
	}

	private String xyt(float[] ts) {
		var xys = Floats_.of(ts).index().map((y, x) -> FltFltPair.of(x, y)).collect();
		return xyt(xys);
	}

	private String xyt(Streamlet<FltFltPair> xys) {
		var xs = xys.map(xy -> xy.t0 + ",").collect(As::joined);
		var ys = xys.map(xy -> xy.t1 + ",").collect(As::joined);
		return "{ x: [" + xs + "], y: [" + ys + "], type: 'scatter', }";
	}

}
