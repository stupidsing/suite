package suite.jdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.function.BiPredicate;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import suite.util.FunUtil.Fun;

public class ClassCreatorTest implements Opcodes {

	@Test
	public void testCreateBiPredicate() {
		@SuppressWarnings("rawtypes")
		ClassCreator<BiPredicate> cc = new ClassCreator<>( //
				BiPredicate.class, //
				"test", //
				Type.getDescriptor(boolean.class), //
				Arrays.asList(Type.getDescriptor(Object.class), Type.getDescriptor(Object.class)));
		@SuppressWarnings("unchecked")
		BiPredicate<Object, Object> bp = cc.create(cc.constant(Boolean.TRUE));
		assertTrue(bp.test("Hello", "world"));
	}

	@Test
	public void testCreateFun() {
		@SuppressWarnings("rawtypes")
		ClassCreator<Fun> cc = new ClassCreator<>( //
				Fun.class, //
				"apply", //
				Type.getDescriptor(Object.class), //
				Arrays.asList(Type.getDescriptor(Object.class)));
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = cc.create(cc.parameter(1));
		assertEquals("Hello", fun.apply("Hello"));
	}

}
