package suite.gpu;

import java.nio.ByteOrder;

import org.bridj.Pointer;
import org.junit.Test;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

public class GpuTest {

	@Test
	public void test() {
		String openCl = "" //
				+ "__kernel void add_floats(__global float *a, __global float *b, __global float *o, int n) { \n" //
				+ "    int i = get_global_id(0); \n" //
				+ "    if (i < n) o[i] = a[i] * a[i] + b[i] * b[i]; \n" //
				+ "} \n" //
		;

		CLContext context = JavaCL.createBestContext();
		CLQueue queue = context.createDefaultQueue();
		ByteOrder byteOrder = context.getByteOrder();

		int n = 1024;
		Pointer<Float> inp0 = Pointer.allocateFloats(n).order(byteOrder);
		Pointer<Float> inp1 = Pointer.allocateFloats(n).order(byteOrder);

		for (int i = 0; i < n; i++) {
			inp0.set(i, (float) Math.cos(i));
			inp1.set(i, (float) Math.sin(i));
		}

		CLBuffer<Float> out = context.createBuffer(Usage.Output, Float.class, n);

		CLKernel kernel = context.createProgram(openCl).createKernel("add_floats");
		kernel.setArgs(context.createBuffer(Usage.Input, inp0), context.createBuffer(Usage.Input, inp1), out, n);

		Pointer<Float> outp = out.read(queue, kernel.enqueueNDRange(queue, new int[] { n, }));

		for (CLDevice device : context.getDevices())
			System.out.println(device);

		for (int i = 0; i < Math.min(10, n); i++)
			System.out.println("out[" + i + "] = " + outp.get(i));
	}

}
