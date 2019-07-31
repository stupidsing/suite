package suite.concurrent;

import static primal.statics.Fail.fail;

import java.util.concurrent.atomic.AtomicStampedReference;

import suite.streamlet.FunUtil.Iterate;

/**
 * A compare-and-set atomic reference that also uses stamp to resolve ABA
 * problem. Actually just a wrapper for atomic stamped reference.
 *
 * The reference would die after ~4 billion generations. After that all updates
 * would fail with exceptions. If your updates are really intensive, you need to
 * replace the reference after some time, or consider other types of
 * concurrency.
 * 
 * @author ywsing
 */
public class CasReference<T> {

	private AtomicStampedReference<T> asr;

	public CasReference(T t) {
		asr = new AtomicStampedReference<>(t, 0);
	}

	public T apply(Iterate<T> fun) {
		while (true) {
			var arr = new int[1];
			var t0 = asr.get(arr);
			var t1 = fun.apply(t0);
			var stamp = arr[0];
			if (stamp != -1)
				if (asr.compareAndSet(t0, t1, stamp, stamp + 1))
					return t1;
				else
					new Backoff().yield();
			else
				return fail("stamp overflow");
		}
	}

	public T getReference() {
		return asr.getReference();
	}

}
