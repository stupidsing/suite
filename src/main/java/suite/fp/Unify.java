package suite.fp;

import java.util.HashMap;
import java.util.Map;

import suite.fp.Unify.UnNode;
import suite.util.AutoObject;
import suite.util.Util;

public class Unify<T extends UnNode<T>> {

	public interface UnNode<T extends UnNode<T>> {
		public boolean unify(UnNode<T> t);

		public default <U extends UnNode<T>> U cast(Class<U> clazz) {
			UnNode<T> t = final_();
			if (clazz.isInstance(t))
				return clazz.cast(t);
			else
				throw new RuntimeException("cannot cast " + t + " to " + clazz);
		}

		public default UnNode<T> final_() {
			UnNode<T> object = this;
			while (true)
				if (object instanceof UnRef) {
					UnNode<T> ref1 = ((UnRef<T>) object).target;
					if (object != ref1)
						object = ref1;
					else
						return object;
				} else
					return object;
		}
	}

	private static class UnRef<T extends UnNode<T>> implements UnNode<T> {
		private UnNode<T> target;
		private int id = Util.temp();

		public boolean unify(UnNode<T> un) {
			return false;
		}
	}

	public UnNode<T> clone(UnNode<T> node) {
		return clone(new HashMap<>(), node);
	}

	public UnNode<T> newRef() {
		UnRef<T> ref = new UnRef<>();
		ref.target = ref;
		return ref;
	}

	public boolean unify(UnNode<T> u0, UnNode<T> u1) {
		UnNode<T> target0 = u0.final_();
		UnNode<T> target1 = u1.final_();
		if (target0 instanceof UnRef)
			return addBind((UnRef<T>) target0, target1);
		else if (target1 instanceof UnRef)
			return addBind((UnRef<T>) target1, target0);
		else {
			@SuppressWarnings("unchecked")
			T t1 = (T) target1;
			return target0.unify(t1);
		}
	}

	public boolean addBind(UnRef<T> reference, UnNode<T> target) {
		if (target instanceof Unify.UnRef) {
			UnRef<T> reference1 = (UnRef<T>) target;
			if (reference.id < reference1.id)
				return bindDirect(reference1, reference);
			else
				return bindDirect(reference, reference1);
		} else
			return bindDirect(reference, target);
	}

	private UnNode<T> clone(Map<UnRef<?>, UnNode<T>> map, UnNode<T> node0) {
		UnNode<T> node1 = node0.final_();
		if (node1 instanceof UnRef)
			return (UnRef<T>) map.computeIfAbsent((UnRef<?>) node1, r -> newRef());
		else {
			@SuppressWarnings("unchecked")
			UnNode<T> object1 = (UnNode<T>) ((AutoObject<?>) node1).clone();
			return object1;
		}
	}

	private boolean bindDirect(UnRef<T> reference, UnNode<T> target) {
		reference.target = target;
		return true;
	}

}
