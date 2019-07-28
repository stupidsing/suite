package suite.rt;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Collection;
import java.util.List;

import suite.math.Math_;
import suite.math.R3;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RtObject;

public class BoundingBox implements RtObject {

	private R3 min, max;
	private RtObject object;

	public BoundingBox(R3 min, R3 max, RtObject object) {
		this.min = min;
		this.max = max;
		this.object = object;
	}

	public static BoundingBox bound(Collection<R3> points, RtObject object) {
		double min = Double.NEGATIVE_INFINITY, max = Double.POSITIVE_INFINITY;
		double minX = max, minY = max, minZ = max;
		double maxX = min, maxY = min, maxZ = min;

		for (var point : points) {
			double x = point.x, y = point.y, z = point.z;
			minX = min(minX, x);
			minY = min(minY, y);
			minZ = min(minZ, z);
			maxX = max(maxX, x);
			maxY = max(maxY, y);
			maxZ = max(maxZ, z);
		}

		return new BoundingBox(new R3(minX, minY, minZ), new R3(maxX, maxY, maxZ), object);
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		float startX = ray.startPoint.x, dirX = ray.dir.x;
		float startY = ray.startPoint.y, dirY = ray.dir.y;
		float startZ = ray.startPoint.z, dirZ = ray.dir.z;
		float minX = min.x, maxX = max.x;
		float minY = min.y, maxY = max.y;
		float minZ = min.z, maxZ = max.z;

		var isIntersect = true //
				&& isIntersect(startX, dirX, minX, maxX, startY, dirY, minY, maxY) //
				&& isIntersect(startY, dirY, minY, maxY, startZ, dirZ, minZ, maxZ) //
				&& isIntersect(startZ, dirZ, minZ, maxZ, startX, dirX, minX, maxX) //
		;

		return isIntersect ? object.hit(ray) : List.of();
	}

	private boolean isIntersect( //
			float startX, float dirX, float minX, float maxX, //
			float startY, float dirY, float minY, float maxY) {
		boolean isIntersect;

		if (Math_.epsilon < abs(dirX)) {
			var gradient = dirY / dirX;
			float y0, y1;

			if (0 < gradient) {
				y0 = (minX - startX) * gradient + startY;
				y1 = (maxX - startX) * gradient + startY;
			} else {
				y0 = (maxX - startX) * gradient + startY;
				y1 = (minX - startX) * gradient + startY;
			}

			isIntersect = minY <= y1 && y0 <= maxY;
		} else
			isIntersect = minX <= startX && startX <= maxX;

		return isIntersect;
	}

}
