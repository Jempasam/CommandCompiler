package jempasam.textanalyzis.reader;

import java.util.Iterator;

public interface SReader<T> extends Iterator<T> {
	void backward();
	void frontward();
}
