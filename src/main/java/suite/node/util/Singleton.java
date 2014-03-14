package suite.node.util;

import suite.util.InspectUtil;
import suite.util.MapifyUtil;

public class Singleton {

	private static Singleton instance = new Singleton();

	private Context grandContext = new Context();
	private Context hiddenContext = new Context(); // For hidden symbols

	private InspectUtil inspectUtil = new InspectUtil();
	private MapifyUtil mapifyUtil = new MapifyUtil(inspectUtil);

	public static Singleton get() {
		return instance;
	}

	private Singleton() {
	}

	public Context getGrandContext() {
		return grandContext;
	}

	public Context getHiddenContext() {
		return hiddenContext;
	}

	public InspectUtil getInspectUtil() {
		return inspectUtil;
	}

	public MapifyUtil getMapifyUtil() {
		return mapifyUtil;
	}

}
