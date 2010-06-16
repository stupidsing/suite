/**
 * 
 */
package org.btree;

import java.nio.ByteBuffer;

public interface ByteBufferAccessor<V> {

	public V read(ByteBuffer buffer);

	public void write(ByteBuffer buffer, V value);

	public static class ByteBufferIntAccessor implements
			ByteBufferAccessor<Integer> {
		public Integer read(ByteBuffer buffer) {
			return buffer.getInt();
		}

		public void write(ByteBuffer buffer, Integer value) {
			buffer.putInt(value);
		}
	}

}
