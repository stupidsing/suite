package suite.rt;

import suite.math.Vector;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayTraceObject;

public class BoundingBox implements RayTraceObject {

	private Vector min, max;
	private RayTraceObject object;

	public BoundingBox(Vector min, Vector max, RayTraceObject object) {
		this.min = min;
		this.max = max;
		this.object = object;
	}

	@Override
	public RayHit hit(final Ray ray) {
		float startX = ray.startPoint.getX();
		float startY = ray.startPoint.getY();
		float startZ = ray.startPoint.getZ();
		float dirX = ray.dir.getX();
		float dirY = ray.dir.getY();
		float dirZ = ray.dir.getZ();

		boolean isOutOfBounds = false //
				|| startX < min.getX() && dirX < 0f //
				|| startY < min.getY() && dirY < 0f //
				|| startZ < min.getZ() && dirZ < 0f //
				|| startX > max.getX() && dirX > 0f //
				|| startY > max.getY() && dirY > 0f //
				|| startZ > max.getZ() && dirZ > 0f;

		return !isOutOfBounds ? object.hit(ray) : null;
	}

}
