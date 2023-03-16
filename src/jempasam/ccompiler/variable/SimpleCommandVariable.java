package jempasam.ccompiler.variable;

import java.util.List;
import java.util.stream.Collectors;
import jempasam.data.chunk.DataChunk;

public class SimpleCommandVariable implements CommandVariable{
	
	
	
	public List<CommandParameter> parameters;
	public DataChunk content;
	
	
	
	public SimpleCommandVariable(List<CommandParameter> parameters, DataChunk content) {
		super();
		this.parameters = parameters;
		this.content = content;
	}
	
	@Override
	public String toString() {
		return "["+parameters.stream().map(param->(param.isReference() ? "&" : "")+param.getName()).collect(Collectors.joining(","))+"]:["+content+"]";
	}
	
	@Override
	public DataChunk getContent() {
		return content;
	}
	
	@Override
	public List<CommandParameter> getParameters() {
		return parameters;
	}
}
