package jempasam.datacompiler.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import jempasam.data.chunk.DataChunk;
import jempasam.logger.SLogger;
import jempasam.samstream.SamStreams;
import jempasam.samstream.stream.SamStream;

public class DataCompiler<C> {
	
	
	
	private Map<String,DataElement<C>> types;
	private Map<DataElement<C>,String> reverseTypes;
	private SLogger logger;
	
	
	
	public DataCompiler(SLogger logger) {
		super();
		this.types=new HashMap<>();
		this.reverseTypes=new HashMap<>();
		this.logger=logger;
	}
	
	
	
	public void addElement(String name, DataElement<C> element) {
		types.put(name, element);
		reverseTypes.put(element,name);
	}
	
	private void compileChild(C context, DataChunk childData, Consumer<Object> registrer, Function<String, DataElement<C>> childTypes) {
		// Get child type
		DataElement<C> childtype=childTypes.apply(childData.getName());
		if(childtype==null)childtype=types.get(childData.getName());
		
		// Add Child
		if(childtype!=null) {
			Object childCompiled=compile(childData,context,childtype);
			addChild(context, childCompiled, registrer, childTypes);
		}		
		else registrer.accept(childData);
	}
	
	private void addChild(C context, Object child, Consumer<Object> registrer, Function<String, DataElement<C>> childTypes) {
		if(child instanceof DataChunk) compileChild(context, (DataChunk)child, registrer, childTypes);
		else if(child instanceof SamStream<?> childStream)childStream.forEach(subChild -> addChild(context, subChild, registrer, childTypes));
		else registrer.accept(child);
	}
	
	public Object compile(DataChunk dc, C context, DataElement<C> type) {
		String name=reverseTypes.get(type);
		if(name==null) {
			if(type.getClass().getDeclaredMethods().length==1)name="unamed";
			else name=type.getName();
		}
		logger.enter().debug(name+" of "+dc);
		
		ChildCompiler<C> childConverter=(childType,chunkstream)->{
			List<Object> parameters=new ArrayList<>();
			for(DataChunk param : chunkstream) {
				compileChild(context, param, parameters::add, childType);
			}
			
			return SamStreams.create(parameters);
		};
		
		
		try {
			Object ret = type.compile(context, dc, childConverter);
			if(ret instanceof Object[] aret)logger.debug("give "+Arrays.toString(aret));
			else logger.debug("give "+ret);
			return ret;
		} catch (DataCompilerException e) {
			logger.error(e.getMessage());
			return null;
		} finally {
			logger.exit();
		}
	}
	
	public Object compile(DataChunk dc, C context) {
		String type=dc.getName();
		DataElement<C> element=types.get(type);
		if(element!=null) return compile(dc,context,element);
		else return null;
	}
}
