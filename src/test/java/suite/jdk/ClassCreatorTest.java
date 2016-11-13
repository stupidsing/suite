package suite.jdk;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import suite.util.FunUtil.Fun;

public class ClassCreatorTest implements Opcodes {

	@Test
	public void testCreateFun() {
		ClassCreator classCreator = new ClassCreator();
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = (Fun<Object, Object>) classCreator.create(classCreator.parameter(1));
		assertEquals("Hello", fun.apply("Hello"));
	}

}
