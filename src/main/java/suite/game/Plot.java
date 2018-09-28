package suite.game;

import java.io.File;

import suite.cfg.Defaults;
import suite.os.Execute;
import suite.os.FileUtil;
import suite.primitive.Floats_;
import suite.primitive.adt.pair.FltFltPair;
import suite.streamlet.As;
import suite.streamlet.Streamlet;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

public class Plot extends ExecutableProgram {

	private String chrome = "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe";

	public static void main(String[] args) {
		RunUtil.run(Plot.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		var d0 = new float[] { 10f, 15f, 13f, 17f, };
		var d1 = new float[] { 16f, 5f, 11f, 9f, };

		var xyt0 = xyt(d0);
		var xyt1 = xyt(d1);

		var html = "" //
				+ "<head><script src='https://cdn.plot.ly/plotly-latest.min.js'></script></head>" //
				+ "<body><div id='plot'></div></body>" //
				+ "<script>" //
				+ "var xyt0 = " + xyt0 + ";" //
				+ "var xyt1 = " + xyt1 + ";" //
				+ "Plotly.newPlot('plot', [xyt0, xyt1], {" //
				+ "	yaxis: { rangemode: 'tozero', zeroline: true, }" //
				+ "});" //
				+ "</script>";

		var file = Defaults.tmp("plot.html");

		FileUtil.out(file).writeAndClose(html);

		if (new File(chrome).exists())
			Execute.shell("'" + chrome + "' --incognito '" + file + "'");

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
