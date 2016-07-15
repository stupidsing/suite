package suite.util;

import java.util.concurrent.SynchronousQueue;

import suite.node.util.Mutable;

/**
 * The synchronous queue class do not support nulls; in this class, we have to
 * use a special object to denote end of data. Thus the real queue needs to be
 * of type Object.
 * 
 * @author ywsing
 */
public class NullableSynchronousQueue<T> {

	private SynchronousQueue<Object> queue = new SynchronousQueue<>();
	private Object nullObject = new Object();

	public void offerQuietly(T t) {
		try {
			offer(t);
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	public boolean poll(Mutable<T> mutable) {
		Object object = queue.poll();
		boolean b = object != nullObject;
		if (b) {
			@SuppressWarnings("unchecked")
			T t = (T) object;
			mutable.set(t);
		}
		return b;
	}

	public T takeQuietly() {
		try {
			return take();
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void offer(T t) throws InterruptedException {
		queue.put(t != null ? t : nullObject);
	}

	public T take() throws InterruptedException {
		Object object = queue.take();
		if (object != nullObject) {
			@SuppressWarnings("unchecked")
			T t = (T) object;
			return t;
		} else
			return null;
	}

}
