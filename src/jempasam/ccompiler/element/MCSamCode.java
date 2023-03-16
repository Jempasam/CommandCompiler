package jempasam.ccompiler.element;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jempasam.ccompiler.CommandContext;
import jempasam.ccompiler.CommandContext.MCSamFile;
import jempasam.ccompiler.element.MCSamProvider.*;
import jempasam.ccompiler.element.type.ValueCode;
import jempasam.ccompiler.variable.CommandParameter;
import jempasam.ccompiler.variable.CommandVariable;
import jempasam.ccompiler.variable.SimpleCommandParameter;
import jempasam.data.chunk.DataChunk;
import jempasam.data.chunk.ObjectChunk;
import jempasam.data.chunk.value.StringChunk;
import jempasam.datacompiler.compiler.ChildCompiler;
import jempasam.datacompiler.compiler.DataCompiler;
import jempasam.datacompiler.compiler.DataCompilerException;
import jempasam.datacompiler.compiler.DataElement;
import jempasam.logger.SLogger;
import jempasam.samstream.SamStreams;

public class MCSamCode {
	
	private static class BaseClass{
		@Override
		public String toString() {
			return getClass().getSimpleName();
		}
	}
	
	
	
	private static class Root extends BaseClass{
		Root(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler) {
			if(data instanceof ObjectChunk oc)childCompiler.compile(oc.childStream());
		}
	}
	
	
	
	private static class Namespace extends BaseClass{
		Namespace(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler) {
			if(data instanceof ObjectChunk oc) {
				String name=oc.getValue(String.class, "name");
				if(name==null)name=Long.toHexString((long)(Math.random()*10000000));
				context.enter(name);
				childCompiler.compile(oc.stream());
				context.exit();
			}
		}
	}
	
	
	
	private static class Token extends BaseClass implements TokenProvider{
		
		private String token;
		
		@Override public String getTokenText() { return token; }
		
		Token(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler){
			if(data instanceof StringChunk sdata) {
				token=sdata.getValue();
				if(token.equals(";"))token=ValueCode.LINEBREAK;
				else if(token.equals(";;"))token=";";
			}
			else token="";
		}
		
		@Override
		public String toString() {
			return "["+token+"]";
		}
	}
	
	
	
	private static class CompleteCode extends BaseClass implements CodeProvider{
		
		public ValueCode code;
		
		private static Map<String,DataElement<CommandContext>> childs=Map.of("",Token::new);
		
		@Override
		public ValueCode getCode() {
			return code;
		}
		
		CompleteCode(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler) {
			
			code=new ValueCode(new ArrayList<>());
			
			if(data instanceof ObjectChunk oc) {
				for(Object o : childCompiler.compile(childs::get, oc.childStream())) {
					if(o instanceof TokenProvider otoken) {
						code.addCode(otoken.getTokenText());
					}
					else if(o instanceof CodeProvider ocode) {
						code.addCode(ocode.getCode());
					}
				}
			}
			
		}
	}
	
	
	
	private static class Import extends BaseClass{
		Import(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler) {
			if(data instanceof ObjectChunk oc) {
				String imported=oc.getValue(String.class, "path");
				context.tryLoadRelative(imported);
			}
		}
	}
	
	
	
	private static class Use extends BaseClass{
		Use(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler) {
			if(data instanceof ObjectChunk oc) {
				String imported=oc.getValue(String.class, "path");
				context.tryLoadLibrary(imported);
			}
		}
	}
	
	
	
	private static class Function extends BaseClass implements TokenProvider{
		
		private String functionName;
		
		@Override public String getTokenText() { return functionName; }
		
		Function(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler) {
			if(data instanceof ObjectChunk oc) {
				String name=oc.getValue(String.class, "name");
				if(name==null)name=Long.toHexString((long)(Math.random()*10000000));
				
				String namespace=oc.getValue(String.class, "in");
				if(namespace!=null)context.enter(namespace);
				
				functionName=context.registerFile(name, ()->{
					return childCompiler.compile(oc.childStream()).filter(CodeProvider.class::isInstance).first().map(CodeProvider.class::cast).map(CodeProvider::getCode).map(ValueCode::getCodeText).get();
				});
				
				if(namespace!=null)context.exit();
			}
			else functionName="";
		}
	}
	
	
	
	private static class Macro extends BaseClass{
		Macro(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler) {
			if(data instanceof ObjectChunk oc) {
				// Name
				String name=oc.getValue(String.class, "name");
				if(name==null)name=Long.toHexString((long)(Math.random()*10000000));
				context.enter(name);
				
				// Code
				DataChunk code=oc.get("code");
				
				// Parameters
				ObjectChunk objparams=oc.getObject("parameters");
				List<CommandParameter> parameters=objparams.childStream()
						.valuesOfType(String.class)
						.<CommandParameter>map(vc->new SimpleCommandParameter(vc.getValue(), vc.getName().equals("reference")))
						.toList();
				context.register("this", code, parameters);
				context.exit();
			}
		}
	}
	
	
	
	private static class Const extends BaseClass{
		Const(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler) {
			if(data instanceof ObjectChunk oc) {
				// Name
				String name=oc.getValue(String.class, "name");
				if(name==null)name=Long.toHexString((long)(Math.random()*10000000));
				context.enter(name);
				
				// Code
				ValueCode code=childCompiler.compile(oc.childStream()).filterCast(CodeProvider.class).first().map(CodeProvider::getCode).get();
				context.register("this", code.getDataChunks().collect(ObjectChunk.collector("code")));
				
				context.exit();
			}
		}
	}
	
	
	
	private static class For extends BaseClass implements CodeProvider{
		
		private ValueCode code;
		
		@Override public ValueCode getCode() {return code;}
		
		
		For(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler) {
			if(data instanceof ObjectChunk oc) {
				
				String name="for-"+Long.toHexString((long)(Math.random()*10000000));
				
				String varname=oc.getValue(String.class, "name");
				
				DataChunk codetocompile=oc.get("code");
				
				List<ValueCode> table=Optional.ofNullable(oc.get("parameters")).map(childCompiler::compile).orElseGet(Optional::empty).map(Parameters.class::cast).map(Parameters::getValues).orElseGet(ArrayList::new);
				
				// Loop
				context.enter(name);
				code=new ValueCode(new ArrayList<>());
				for(ValueCode vcode: table) {
					context.register(varname, vcode.getDataChunks().collect(ObjectChunk.collector("code")));
					code.addCode(childCompiler.compile(codetocompile).map(CodeProvider.class::cast).map(CodeProvider::getCode).get());
				}
				context.exit();
			}
		}
	}
	
	
	
	private static class Parameters extends BaseClass{
		
		private List<ValueCode> parameters;
		
		public List<ValueCode> getValues(){
			return parameters;
		}
		
		Parameters(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler) {
			context.enterHidden("parameters");
			parameters=new ArrayList<>();
			
			if(data instanceof ObjectChunk odata) {
				for(Object obj : childCompiler.compile(odata.childStream())) {
					if(obj instanceof CodeProvider ocode) {
						System.out.println("TROP "+data);;
						parameters.add(ocode.getCode());
					}
				}
			}
			
			context.exit();
		}
	}
	
	
	
	private static class Patch extends BaseClass implements CodeProvider{
		
		private ValueCode code;
		
		@Override public ValueCode getCode() { return code; }
				
		Patch(CommandContext context, DataChunk data, ChildCompiler<CommandContext> childCompiler) throws DataCompilerException{
			context.enterHidden("patch-"+Long.toHexString((long)(Math.random()*10000000)));
			if(data instanceof ObjectChunk odata) {
				List<ValueCode> values=Optional.ofNullable(odata.get("parameters")).map(childCompiler::compile).orElseGet(Optional::empty).map(Parameters.class::cast).map(Parameters::getValues).orElseGet(ArrayList::new);
				
				String name=odata.getValue(String.class, "name");
				CommandVariable variable=context.get(name,values.size());
				if(variable!=null) {
					List<CommandParameter> parameters=variable.getParameters();
					for(int i=0; i<values.size(); i++) {
						System.out.println("TERPA "+values);;
						context.register(parameters.get(i).getName(), values.get(i).getDataChunks().collect(ObjectChunk.collector("code")));
					}
					code=childCompiler.compile(SamStreams.create(variable.getContent())).filterCast(CodeProvider.class).map(cp->cp.getCode()).first().get();
				}
				else {
					code=new ValueCode(new ArrayList<>());
					context.exit();
					throw new DataCompilerException("Variable \""+name+"\" of size \""+values.size()+"\" does not exist.");
				}
			}
			context.exit();
		}
	}
	
	
	
	public static DataCompiler<CommandContext> createCompiler(SLogger logger){
		DataCompiler<CommandContext> compiler=new DataCompiler<>(logger);
		compiler.addElement("root", Root::new);
		compiler.addElement("import", Import::new);
		compiler.addElement("code", CompleteCode::new);
		compiler.addElement("use", Use::new);
		compiler.addElement("function", Function::new);
		compiler.addElement("namespace", Namespace::new);
		compiler.addElement("patch", Patch::new);
		compiler.addElement("macro", Macro::new);
		compiler.addElement("parameters", Parameters::new);
		compiler.addElement("const", Const::new);
		compiler.addElement("for", For::new);
		return compiler;
	}
	
	public static void saveFile(Path rootpath, MCSamFile file) throws IOException {
		String[] splited=file.name.split(":",2);
		String funcpath=splited[1]+".mcfunction";
		
		Path savepath=rootpath.resolve(funcpath);
		savepath.getParent().toFile().mkdirs();
		File savefile=savepath.toFile();
		
		try(BufferedWriter output=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(savefile)))){
			output.write(file.content);
		}
	}
	
}
