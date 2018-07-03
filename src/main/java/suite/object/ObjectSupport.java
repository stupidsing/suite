package suite.object;

import java.util.List;
import java.util.Objects;

import suite.immutable.IList;
import suite.streamlet.FunUtil.Fun;
import suite.util.Object_;
import suite.util.String_;

public class ObjectSupport<T> {

	private static ThreadLocal<IList<Object>> recurse = ThreadLocal.withInitial(IList::end);

	private Fun<T, List<?>> listFun;

	public ObjectSupport(Fun<T, List<?>> listFun) {
		this.listFun = listFun;
	}

	public int compare(T t0, T t1) {
		var class0 = t0.getClass();
		var class1 = t1.getClass();
		int c;
		if (class0 == class1) {
			var iter0 = listFun.apply(t0).iterator();
			var iter1 = listFun.apply(t1).iterator();
			boolean b0, b1;
			c = 0;
			while (c == 0 && (c = Boolean.compare(b0 = iter0.hasNext(), b1 = iter1.hasNext())) == 0)
				if (b0 && b1)
					c = Object_.compareAnyway(iter0.next(), iter1.next());
		} else
			c = String_.compare(class0.getName(), class1.getName());
		return c;
	}

	public boolean isEquals(T t0, Object object) {
		boolean b;
		if (t0.getClass() == object.getClass()) {
			@SuppressWarnings("unchecked")
			var t1 = (T) object;
			b = equals(t0, t1);
		} else
			b = false;
		return b;
	}

	public boolean equals(T t0, T t1) {
		var list0 = listFun.apply(t0);
		var list1 = listFun.apply(t1);
		var size0 = list0.size();
		var size1 = list1.size();
		var b = true;
		if (size0 == size1)
			for (var i = 0; i < size0; i++)
				b &= Objects.equals(list0.get(i), list1.get(i));
		return b;
	}

	public int hashCode(T t) {
		var h = 7;
		for (var value : listFun.apply(t))
			h = h * 31 + Objects.hashCode(value);
		return h;

	}

	public String toString(T t) {
		var recurse0 = recurse.get();
		var sb = new StringBuilder();

		if (!recurse0.containsId(t))
			try {
				recurse.set(IList.cons(t, recurse0));
				sb.append(t.getClass().getSimpleName() + "(");
				for (var value : listFun.apply(t))
					sb.append(value + ",");
				sb.append(")");
				return sb.toString();
			} finally {
				recurse.set(recurse0);
			}
		else
			return "<recurse>";
	}

}
