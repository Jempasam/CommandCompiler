package jempasam.datacompiler.compiler;

import java.util.Optional;
import java.util.function.Function;

import jempasam.data.chunk.DataChunk;
import jempasam.samstream.SamStreams;
import jempasam.samstream.stream.SamStream;

public interface ChildCompiler<C> {
	SamStream<Object> compile(Function<String,DataElement<C>> childTypes, SamStream<DataChunk> childs);
	
	@SuppressWarnings("unchecked")
	default SamStream<Object> compile(SamStream<DataChunk> childs){
		return compile((Function<String, DataElement<C>>)EMPTY,childs);
	}
	
	default Optional<Object> compile(Function<String,DataElement<C>> childTypes, DataChunk child){
		return compile(childTypes, SamStreams.create(child)).first();
	}
	
	@SuppressWarnings("unchecked")
	default Optional<Object> compile(DataChunk child){
		return compile((Function<String, DataElement<C>>)EMPTY, child);
	}
	
	
	
	@SuppressWarnings("rawtypes")
	public static Function EMPTY=str->null;
}
