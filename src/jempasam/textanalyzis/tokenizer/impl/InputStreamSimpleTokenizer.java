package jempasam.textanalyzis.tokenizer.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import jempasam.textanalyzis.tokenizer.Tokenizer;



public class InputStreamSimpleTokenizer implements Tokenizer{
	private Reader input;
	private String separator;
	private String solo;
	private String stringsep;
	private String endsep;
	
	private String comment="";
	
	private int[] buffer;
	private int buffer_last;
	private int buffer_end;
	private String nextword=null;
	private boolean first=true;
	
	public InputStreamSimpleTokenizer(Reader input, String separator, String solo_character, String stringsep, String endsep) {
		this.input=input;
		this.separator=separator;
		this.solo=solo_character;
		this.stringsep=stringsep;
		this.endsep=endsep;
		
		buffer=new int[10];
		buffer_last=0;
		buffer_end=0;
	}
	
	public InputStreamSimpleTokenizer(InputStream input, String separator, String solo_character, String stringsep, String endsep) {
		this(new InputStreamReader(input), separator, solo_character, stringsep, endsep);
		
	}
	
	public InputStreamSimpleTokenizer(InputStream input, String separator, String solo_character, String stringsep) {
		this(input,separator,solo_character,stringsep,"");
	}
	
	public String getComment() { return comment; }
	public void setComment(String comment) { this.comment = comment; }

	private int nextchar() {
		if(buffer_last==buffer_end){
			buffer_last++;
			if(buffer_last>=buffer.length)buffer_last=0;
			buffer_end=buffer_last;
			try {
				buffer[buffer_last]=input.read();
			} catch (IOException e) {
				buffer[buffer_last]=-1;
			}
		}else {
			buffer_last++;
			if(buffer_last>=buffer.length)buffer_last=0;
		}
		return buffer[buffer_last];
	}
	
	private void backward() {
		buffer_last--;
		if(buffer_last<0)buffer_last=buffer.length-1;
	}
	
	private void eatnext() {
		StringBuilder sb=new StringBuilder();
		int ch;
		
		//READ THE TOKEN
		int instring=-1;
		int incomment=-1;
		
		while((ch=nextchar())!=-1) {
			if(instring==-1) {
				if(incomment!=-1) {
					if(ch==incomment)incomment=-1;
				}
				else if(comment.indexOf(ch)!=-1) {
					incomment=ch;
				}
				else if(separator.indexOf(ch)!=-1) {
					if(sb.length()>0)break;
				}
				else if(endsep.indexOf(ch)!=-1) {
					sb.append((char)ch);
					break;
				}
				else if(solo.indexOf(ch)!=-1) {
					if(sb.length()>0) {
						backward();
						break;
					}
					else {
						sb.append((char)ch);
						break;
					}
				}
				else if(stringsep.indexOf(ch)!=-1)instring=ch;
				else sb.append((char)ch);
			}
			else {
				if(ch==instring)instring=-1;
				else sb.append((char)ch);
			}
		}
		
		if(sb.length()==0)nextword=null;
		else nextword=sb.toString();
	}
	
	@Override
	public String next() {
		if(first) {
			eatnext();
			first=false;
		}
		String ret=nextword;
		eatnext();
		return ret;
	}
	
	@Override
	public boolean hasNext() {
		if(first) {
			eatnext();
			first=false;
		}
		return nextword!=null;
	}
}
