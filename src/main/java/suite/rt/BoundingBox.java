package suite.rt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import suite.math.MathUtil;
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

	public static BoundingBox bound(Collection<Vector> points, RayTraceObject object) {
		float min = Float.NEGATIVE_INFINITY, max = Float.POSITIVE_INFINITY;
		float minX = max, minY = max, minZ = max;
		float maxX = min, maxY = min, maxZ = min;

		for (Vector point : points) {
			float x = point.getX(), y = point.getY(), z = point.getZ();
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			minZ = Math.min(minZ, z);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
			maxZ = Math.max(maxZ, z);
		}

		return new BoundingBox(new Vector(minX, minY, minZ), new Vector(maxX, maxY, maxZ), object);
	}

	@Override
	public List<RayHit> hit(final Ray ray) {
		float startX = ray.startPoint.getX(), dirX = ray.dir.getX();
		float startY = ray.startPoint.getY(), dirY = ray.dir.getY();
		float startZ = ray.startPoint.getZ(), dirZ = ray.dir.getZ();
		float minX = min.getX(), maxX = max.getX();
		float minY = min.getY(), maxY = max.getY();
		float minZ = min.getZ(), maxZ = max.getZ();

		boolean isIntersect = true //
				&& isIntersect(startX, dirX, minX, maxX, startY, dirY, minY, maxY) //
				&& isIntersect(startY, dirY, minY, maxY, startZ, dirZ, minZ, maxZ) //
				&& isIntersect(startZ, dirZ, minZ, maxZ, startX, dirX, minX, maxX) //
		;

		return isIntersect ? object.hit(ray) : Collections.<RayHit> emptyList();
	}

	private boolean isIntersect(float startX, float dirX, float minX, float maxX, float startY, float dirY, float minY, float maxY) {
		if (Math.abs(dirX) > MathUtil.epsilon) {
			float gradient = dirY / dirX;
			float y0_ = (minX - startX) * gradient + startY;
			float y1_ = (maxX - startX) * gradient + startY;

			float y0, y1;

			if (y0_ < y1_) {
				y0 = y0_;
				y1 = y1_;
			} else {
				y0 = y1_;
				y1 = y0_;
			}

			return minY <= y1 && y0 <= maxY;
		} else
			return minX <= startX && startX <= maxX;
	}

}
