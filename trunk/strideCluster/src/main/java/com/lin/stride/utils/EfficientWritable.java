package com.lin.stride.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface EfficientWritable {

	void writeData(DataOutput out) throws IOException;

	void readDate(DataInput in) throws IOException;

}
