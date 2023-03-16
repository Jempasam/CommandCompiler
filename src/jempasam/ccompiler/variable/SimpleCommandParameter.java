package jempasam.ccompiler.variable;

public class SimpleCommandParameter implements CommandParameter{
	
	
	
	private String name;
	private boolean isReference;
	
	
	
	public SimpleCommandParameter(String name, boolean isReference) {
		super();
		this.name = name;
		this.isReference = isReference;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public boolean isReference() {
		return isReference;
	}
	
}
