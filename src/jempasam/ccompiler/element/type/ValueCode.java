package jempasam.ccompiler.element.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jempasam.data.chunk.DataChunk;
import jempasam.data.chunk.value.StringChunk;
import jempasam.samstream.SamStreams;
import jempasam.samstream.stream.SamStream;

public class ValueCode {
	
	
	
	public static String LINEBREAK="\n";
	
	private static Set<String> nospace=Set.of(":","{","}",",",".","=","[",";");
	private static Set<String> nospaceAfter=Set.of();
	private static Set<String> nospaceBefore=Set.of("]");
	private static StringChunk LINE_SEPARATOR=new StringChunk("", LINEBREAK);
	
	private List<List<String>> tokens;
	
	
	
	public ValueCode(List<List<String>> tokens) {
		this.tokens=tokens;
	}
	
	
	
	public String getCodeText() {
		StringBuilder sb=new StringBuilder();
		for(List<String> line : tokens) {
			String previous=null;
			boolean insideText=false;
			boolean inid=false;
			for(String token : line) {
				String val=token;
				if(previous!=null) {
					String afterChar=val.substring(0, 1);
					String beforeChar=previous.substring(previous.length()-1,previous.length());
					if(afterChar.equals("{")&&!inid)sb.append(" ");
					else if(afterChar.equals("\"")) {
						if(!insideText) sb.append(" ");
						insideText=!insideText;
					}
					else if(beforeChar.equals("\"")) {
						if(!insideText)sb.append(" ");
					}
					else if(nospace.contains(beforeChar)||nospaceAfter.contains(beforeChar));
					else if(nospace.contains(afterChar)||nospaceBefore.contains(afterChar));
					else sb.append(" ");
					
					if(beforeChar.equals(":"))inid=true;
					else inid=false;
				}
				sb.append(token);
				previous=val;
			}
			sb.append("\n");
		}
		if(sb.length()>1)sb.setLength(sb.length()-2);
		return sb.toString();
	}
	
	public void addCode(ValueCode added) {
		if(tokens.size()==0)tokens.add(new ArrayList<>());
		for(List<String> lineAdded : added.tokens) {
			tokens.get(tokens.size()-1).addAll(lineAdded);
			tokens.add(new ArrayList<>());
		}
		tokens.remove(tokens.size()-1);
	}
	
	public void addCode(String token) {
		if(tokens.size()==0)tokens.add(new ArrayList<>());
		if(token.equals(LINEBREAK)) {
			if(tokens.get(tokens.size()-1).size()>0)tokens.add(new ArrayList<>());
		}
		else tokens.get(tokens.size()-1).add(token);
	}
	
	public void addNewLine() {
		addCode(LINEBREAK);
	}
	
	public SamStream<DataChunk> getDataChunks(){
		return SamStreams.create(tokens).flatMap( line->SamStreams.create(line).<DataChunk>map(str->new StringChunk("", str)).after(SamStreams.create(LINE_SEPARATOR))).skip(1);
	}
	
	@Override
	public String toString() {
		return tokens.toString();
	}
	
}
