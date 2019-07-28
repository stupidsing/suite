package suite.gpu;

import static java.lang.Math.min;

import org.bridj.Pointer;
import org.junit.Test;

import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.JavaCL;

public class GpuTest {

	@Test
	public void test() {
		var openCl = "" //
				+ "__kernel void add_floats(__global float *a, __global float *b, __global float *o, int n) { \n" //
				+ "    int i = get_global_id(0); \n" //
				+ "    if (i < n) o[i] = a[i] * a[i] + b[i] * b[i]; \n" //
				+ "} \n" //
		;

		var context = JavaCL.createBestContext();
		var queue = context.createDefaultQueue();
		var byteOrder = context.getByteOrder();

		var n = 1024;
		var inp0 = Pointer.allocateFloats(n).order(byteOrder);
		var inp1 = Pointer.allocateFloats(n).order(byteOrder);

		for (var i = 0; i < n; i++) {
			inp0.set(i, (float) Math.cos(i));
			inp1.set(i, (float) Math.sin(i));
		}

		var out = context.createBuffer(Usage.Output, Float.class, n);

		var kernel = context.createProgram(openCl).createKernel("add_floats");
		kernel.setArgs(context.createBuffer(Usage.Input, inp0), context.createBuffer(Usage.Input, inp1), out, n);

		var outp = out.read(queue, kernel.enqueueNDRange(queue, new int[] { n, }));

		for (var device : context.getDevices())
			System.out.println(device);

		for (var i = 0; i < min(10, n); i++)
			System.out.println("out[" + i + "] = " + outp.get(i));
	}

}
