package jempasam.datacompiler.compiler;

import jempasam.data.chunk.DataChunk;

@FunctionalInterface
public interface DataElement<C> {
	Object compile(C context, DataChunk data, ChildCompiler<C> childCompiler) throws DataCompilerException;
	default String getName() {
		return getClass().getSimpleName();
	}
}
