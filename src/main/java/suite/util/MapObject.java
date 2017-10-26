package suite.util;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Fixie_.FixieFun5;
import suite.adt.pair.Fixie_.FixieFun6;
import suite.adt.pair.Fixie_.FixieFun7;
import suite.adt.pair.Fixie_.FixieFun8;
import suite.adt.pair.Fixie_.FixieFun9;
import suite.adt.pair.Fixie_.FixieFunA;
import suite.streamlet.Read;

public abstract class MapObject<T extends MapObject<T>> implements Cloneable, Comparable<T> {

	public static List<?> list(Object object) {
		Class<?> clazz = object.getClass();

		Method m = Read.from(clazz.getMethods()) //
				.filter(method -> String_.equals(method.getName(), "apply")) //
				.uniqueResult();

		Class<?> type = m.getParameters()[0].getType();
		Object p;

		if (type == FixieFun0.class)
			p = (FixieFun0<?>) List::of;
		else if (type == FixieFun1.class)
			p = (FixieFun1<?, ?>) List::of;
		else if (type == FixieFun2.class)
			p = (FixieFun2<?, ?, ?>) List::of;
		else if (type == FixieFun3.class)
			p = (FixieFun3<?, ?, ?, ?>) List::of;
		else if (type == FixieFun4.class)
			p = (FixieFun4<?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun5.class)
			p = (FixieFun5<?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun6.class)
			p = (FixieFun6<?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun7.class)
			p = (FixieFun7<?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun8.class)
			p = (FixieFun8<?, ?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun9.class)
			p = (FixieFun9<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFunA.class)
			p = (FixieFunA<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else
			throw new RuntimeException();

		return (List<?>) Rethrow.ex(() -> m.invoke(object, p));
	}

	@Override
	public MapObject<T> clone() {
		return Rethrow.ex(() -> {
			List<?> list = list(this);
			@SuppressWarnings("unchecked")
			MapObject<T> t1 = (MapObject<T>) getClass().getMethod("of").invoke(null, list.toArray());
			return t1;
		});
	}

	@Override
	public int compareTo(T t1) {
		Class<?> class0 = getClass();
		Class<?> class1 = t1.getClass();
		int c;
		if (class0 == class1) {
			T t0 = self();
			Iterator<Comparable<?>> iter0 = t0.values().iterator();
			Iterator<Comparable<?>> iter1 = t1.values().iterator();
			boolean b0, b1;
			c = 0;
			while (c == 0 && (c = Boolean.compare(b0 = iter0.hasNext(), b1 = iter1.hasNext())) == 0)
				if (b0 && b1) {
					@SuppressWarnings("unchecked")
					Comparable<Object> value0 = (Comparable<Object>) iter0.next();
					@SuppressWarnings("unchecked")
					Comparable<Object> value1 = (Comparable<Object>) iter1.next();
					c = value0.compareTo(value1);
				}
		} else
			c = Object_.compare(class0.getName(), class1.getName());
		return c;
	}

	@Override
	public boolean equals(Object object) {
		boolean b;
		if (getClass() == object.getClass()) {
			T t0 = self();
			@SuppressWarnings("unchecked")
			T t1 = (T) object;
			List<Comparable<?>> values0 = t0.values();
			List<Comparable<?>> values1 = t1.values();
			int size0 = values0.size();
			int size1 = values1.size();
			b = true;
			if (size0 == size1)
				for (int i = 0; i < size0; i++)
					b &= Objects.equals(values0.get(i), values1.get(i));
		} else
			b = false;
		return b;
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		for (Comparable<?> value : values())
			hashCode = 31 * hashCode + Objects.hashCode(value);
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName() + "(");
		for (Object value : values())
			sb.append(value + ",");
		sb.append(")");
		return sb.toString();
	}

	public List<Comparable<?>> values() {
		List<?> list0 = list(this);
		@SuppressWarnings("unchecked")
		List<Comparable<?>> list1 = (List<Comparable<?>>) list0;
		return list1;
	}

	private T self() {
		@SuppressWarnings("unchecked")
		T t = (T) this;
		return t;
	}

}
