package suite.util;

import static primal.statics.Rethrow.ex;

import java.util.concurrent.SynchronousQueue;

import suite.adt.Mutable;

/**
 * The synchronous queue class do not support nulls; in this class, we have to
 * use a special object to denote end of data. Thus the real queue needs to be
 * of type Object.
 * 
 * @author ywsing
 */
public class NullableSyncQueue<T> {

	private SynchronousQueue<Object> queue = new SynchronousQueue<>();
	private Object nullObject = new Object();

	public void offerQuietly(T t) {
		ex(() -> {
			offer(t);
			return t;
		});
	}

	public boolean poll(Mutable<T> mutable) {
		var object = queue.poll();
		var b = object != nullObject;
		if (b) {
			@SuppressWarnings("unchecked")
			var t = (T) object;
			mutable.set(t);
		}
		return b;
	}

	public T takeQuietly() {
		return ex(this::take);
	}

	public void offer(T t) throws InterruptedException {
		queue.put(t != null ? t : nullObject);
	}

	public T take() throws InterruptedException {
		var object = queue.take();
		if (object != nullObject) {
			@SuppressWarnings("unchecked")
			var t = (T) object;
			return t;
		} else
			return null;
	}

}
