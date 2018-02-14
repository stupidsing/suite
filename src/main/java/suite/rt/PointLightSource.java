package suite.rt;

import suite.math.R3;
import suite.rt.RayTracer.LightSource;

public class PointLightSource implements LightSource {

	private R3 source;
	private R3 lum;

	public PointLightSource(R3 source, R3 lum) {
		this.source = source;
		this.lum = lum;
	}

	@Override
	public R3 source() {
		return source;
	}

	@Override
	public R3 lit(R3 point) {
		return lum;
	}

}
