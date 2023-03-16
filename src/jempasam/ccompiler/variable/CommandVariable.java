package jempasam.ccompiler.variable;

import java.util.List;
import jempasam.data.chunk.DataChunk;

public interface CommandVariable {
	
	List<CommandParameter> getParameters();
	
	DataChunk getContent();
	
}
