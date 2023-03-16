package jempasam.ccompiler.element;

import jempasam.ccompiler.element.type.ValueCode;

public class MCSamProvider {
	
	private MCSamProvider() {}
	
	
	
	public static interface TokenProvider{
		String getTokenText();
	}
	
	public static interface CodeProvider{
		ValueCode getCode();
	}
	
	
	
}
