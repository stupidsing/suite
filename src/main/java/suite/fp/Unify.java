package suite.fp;

import java.util.HashMap;
import java.util.Map;

import suite.fp.Unify.UnNode;
import suite.object.AutoObject;
import suite.util.Fail;
import suite.util.Util;

public class Unify<T extends UnNode<T>> {

	public interface UnNode<T extends UnNode<T>> {
		public boolean unify(UnNode<T> t);

		public default <U extends UnNode<T>> U cast(Class<U> clazz) {
			var t = final_();
			return clazz.isInstance(t) ? clazz.cast(t) : Fail.t("cannot cast " + t + " to " + clazz);
		}

		public default UnNode<T> final_() {
			UnNode<T> refTarget;
			return this instanceof UnRef && (refTarget = ((UnRef<T>) this).target) != this ? refTarget.final_() : this;
		}
	}

	private static class UnRef<T extends UnNode<T>> extends AutoObject<UnRef<T>> implements UnNode<T> {
		private UnNode<T> target = this;
		private int id = Util.temp();

		public boolean unify(UnNode<T> un) {
			return false;
		}
	}

	public UnNode<T> clone(UnNode<T> node) {
		return clone(new HashMap<>(), node);
	}

	public UnNode<T> newRef() {
		return new UnRef<>();
	}

	public boolean unify(UnNode<T> u0, UnNode<T> u1) {
		var target0 = u0.final_();
		var target1 = u1.final_();
		if (target0 instanceof UnRef)
			return addBind((UnRef<T>) target0, target1);
		else if (target1 instanceof UnRef)
			return addBind((UnRef<T>) target1, target0);
		else {
			@SuppressWarnings("unchecked")
			var t1 = (T) target1;
			return target0.unify(t1);
		}
	}

	public boolean addBind(UnRef<T> reference, UnNode<T> target) {
		if (target instanceof Unify.UnRef) {
			var reference1 = (UnRef<T>) target;
			if (reference.id < reference1.id)
				return bindDirect(reference1, reference);
			else
				return bindDirect(reference, reference1);
		} else
			return bindDirect(reference, target);
	}

	private UnNode<T> clone(Map<UnRef<?>, UnNode<T>> map, UnNode<T> node0) {
		var node1 = node0.final_();
		if (node1 instanceof UnRef)
			return (UnRef<T>) map.computeIfAbsent((UnRef<?>) node1, r -> newRef());
		else {
			@SuppressWarnings("unchecked")
			var object1 = (UnNode<T>) ((AutoObject<?>) node1).clone();
			return object1;
		}
	}

	private boolean bindDirect(UnRef<T> reference, UnNode<T> target) {
		reference.target = target;
		return true;
	}

}
