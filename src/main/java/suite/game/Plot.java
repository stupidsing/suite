package suite.game;

import java.io.File;

import suite.cfg.Defaults;
import suite.os.Execute;
import suite.os.FileUtil;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

public class Plot extends ExecutableProgram {

	private String chrome = "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe";

	public static void main(String[] args) {
		RunUtil.run(Plot.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		var html = "" //
				+ "<head><script src='https://cdn.plot.ly/plotly-latest.min.js'></script></head>" //
				+ "<body><div id='plot'></div></body>" //
				+ "<script>" //
				+ "var xyt0 = {" //
				+ "	x: [1, 2, 3, 4,]," //
				+ "	y: [10, 15, 13, 17,]," //
				+ "	type: 'scatter'," //
				+ "};" //
				+ "var xyt1 = {" //
				+ "	x: [1, 2, 3, 4,]," //
				+ "	y: [16, 5, 11, 9,]," //
				+ "	type: 'scatter'," //
				+ "};" //
				+ "Plotly.newPlot('plot', [xyt0, xyt1], {" //
				+ "	yaxis: { rangemode: 'tozero', showline: true, zeroline: true, }" //
				+ "});" //
				+ "</script>";

		var file = Defaults.tmp("plot.html");

		FileUtil.out(file).writeAndClose(html);

		if (new File(chrome).exists())
			Execute.shell("'" + chrome + "' --incognito '" + file + "'");

		return true;
	}

}
