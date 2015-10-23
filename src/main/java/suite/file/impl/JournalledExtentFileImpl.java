package suite.file.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import suite.file.ExtentAllocator.Extent;
import suite.file.ExtentFile;
import suite.util.SerializeUtil.Serializer;

public class JournalledExtentFileImpl extends JournalledDataFileImpl<Extent>implements ExtentFile {

	private static Serializer<Extent> extentSerializer = new Serializer<Extent>() {
		public Extent read(DataInput dataInput) throws IOException {
			int start = dataInput.readInt();
			int count = dataInput.readInt();
			return new Extent(start, count);
		}

		public void write(DataOutput dataOutput, Extent extent) throws IOException {
			dataOutput.writeInt(extent.start);
			dataOutput.writeInt(extent.count);
		}
	};

	public JournalledExtentFileImpl(String filename, int maxExtentSize) throws IOException {
		super( //
				new ExtentFileImpl(filename, maxExtentSize) //
				, new PageFileImpl(filename + ".journal", maxExtentSize + 8) //
				, new PageFileImpl(filename + ".pointer", 8) //
				, maxExtentSize //
				, extentSerializer);
	}

}
