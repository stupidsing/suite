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

public class MapObject_ {

	public static <T extends MapObject<T>> int compare(T t0, T t1) {
		Class<?> class0 = t0.getClass();
		Class<?> class1 = t1.getClass();
		int c;
		if (class0 == class1) {
			@SuppressWarnings("unchecked")
			Iterator<Comparable<Object>> iter0 = (Iterator<Comparable<Object>>) list(t0).iterator();
			@SuppressWarnings("unchecked")
			Iterator<Comparable<Object>> iter1 = (Iterator<Comparable<Object>>) list(t1).iterator();
			boolean b0, b1;
			c = 0;
			while (c == 0 && (c = Boolean.compare(b0 = iter0.hasNext(), b1 = iter1.hasNext())) == 0)
				if (b0 && b1) {
					Comparable<Object> value0 = iter0.next();
					Comparable<Object> value1 = iter1.next();
					c = value0.compareTo(value1);
				}
		} else
			c = Object_.compare(class0.getName(), class1.getName());
		return c;
	}

	public static <T extends MapObject<T>> boolean equals(T t0, T t1) {
		List<?> list0 = list(t0);
		List<?> list1 = list(t1);
		int size0 = list0.size();
		int size1 = list1.size();
		boolean b = true;
		if (size0 == size1)
			for (int i = 0; i < size0; i++)
				b &= Objects.equals(list0.get(i), list1.get(i));
		return b;
	}

	public static <T extends MapObject<T>> MapObject<T> construct(Class<?> clazz, List<?> list) {
		return Rethrow.ex(() -> {
			int size = list.size();
			Method m = Read //
					.from(clazz.getMethods()) //
					.filter(method -> String_.equals(method.getName(), "of") && method.getParameterCount() == size) //
					.uniqueResult();
			@SuppressWarnings("unchecked")
			MapObject<T> t = (MapObject<T>) m.invoke(null, list.toArray());
			return t;
		});
	}

	public static <T extends MapObject<T>> List<?> list(Object object) {
		Class<?> clazz = object.getClass();

		Method m = Read //
				.from(clazz.getMethods()) //
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
			p = Fail.t();

		return (List<?>) Rethrow.ex(() -> m.invoke(object, p));
	}

}
