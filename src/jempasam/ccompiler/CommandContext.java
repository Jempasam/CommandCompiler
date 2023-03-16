package jempasam.ccompiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import jempasam.ccompiler.element.MCSamCode;
import jempasam.ccompiler.variable.CommandParameter;
import jempasam.ccompiler.variable.CommandVariable;
import jempasam.ccompiler.variable.SimpleCommandVariable;
import jempasam.data.chunk.DataChunk;
import jempasam.data.chunk.ObjectChunk;
import jempasam.data.chunk.value.StringChunk;
import jempasam.data.deserializer.DataDeserializer;
import jempasam.data.deserializer.DataDeserializers;
import jempasam.data.deserializer.language.LanguageDataDeserializer;
import jempasam.data.deserializer.language.SimpleLanguageLoader;
import jempasam.data.deserializer.language.TokenType;
import jempasam.data.serializer.DataSerializer;
import jempasam.data.serializer.JsonDataSerializer;
import jempasam.datacompiler.compiler.DataCompiler;
import jempasam.logger.SLogger;
import jempasam.samstream.SamStreams;
import jempasam.samstream.collectors.SamCollectors;
import jempasam.samstream.text.TokenizerConfig;

public class CommandContext {
	
	
	
	public String namespace;
	public List<PathPart> path;
	public Map<String,CommandVariable> variables;
	private Map<String,MCSamFile> files;
	private Set<String> loadedFiles;
	private List<Path> pathOffset;
	private List<String> filename;
	
	public final SLogger logger;
	private static TokenizerConfig tokenizerConfig=new TokenizerConfig().setCommenter("#").setSpliter("\t\r ").setAlone("{}[]()=.,:\"'\n").setFirstOfToken("<").setLastOfToken(">").setGroupAlone("&%;");
	private DataCompiler<CommandContext> compiler;
	private TokenType language;
	
	public CommandContext(String namespace, Path rootPath, SLogger langLogger, SLogger deserializerLogger) {
		super();
		this.namespace = namespace;
		this.path = new ArrayList<>();
		this.variables=new HashMap<>();
		this.files=new HashMap<>();
		this.loadedFiles=new HashSet<>();
		this.pathOffset=new ArrayList<>();
		this.filename=new ArrayList<>();
		this.pathOffset.add(rootPath.resolve("."));
		
		this.logger=deserializerLogger;
		DataDeserializer languageDeserializer=DataDeserializers.createJSONLikeStrobjoDS(langLogger);
		SimpleLanguageLoader languageLoader=new SimpleLanguageLoader(langLogger);
		
		langLogger.enter().debug("\n\nLoad Language");
		langLogger.debug("Get Input");
		InputStream input=CCMain.class.getClassLoader().getResourceAsStream("jempasam/language.swjson");
		langLogger.debug("Get Data");
		ObjectChunk languageData=languageDeserializer.loadFrom(input);
		languageData.childStream().objects().forEach(d->d.childStream().objects().forEach(oc->oc.add(new StringChunk("name", oc.getName()))));
		langLogger.debug("-->"+languageData.toString());
		langLogger.debug("Load Data");
			languageLoader.load(languageData);	
			langLogger.debug("-->"+languageLoader.tokenPredicates().stream().map(Object::toString).collect(SamCollectors.concatenate("\n")));
			langLogger.debug("-->"+languageLoader.tokenTypes().stream().map(Object::toString).collect(SamCollectors.concatenate("\n")));
			langLogger.exit();
		this.language=languageLoader.tokenTypes().get("filestart");
		this.compiler=MCSamCode.createCompiler(deserializerLogger);
		
		register("rand", new CommandVariable() {
			private Random rand=new Random();
			@Override public List<CommandParameter> getParameters() { return Collections.emptyList(); }
			@Override public DataChunk getContent() {
				return SamStreams.create(new StringChunk("", Integer.toString(rand.nextInt(30000)))).collect(ObjectChunk.collector("code"));
			}
		});
		
		register("counter", new CommandVariable() {
			private int counter=0;
			@Override public List<CommandParameter> getParameters() { return Collections.emptyList(); }
			@Override public DataChunk getContent() {
				counter++;
				return SamStreams.create(new StringChunk("", Integer.toString(counter))).collect(ObjectChunk.collector("code"));
			}
		});
		
		register("namespace", SamStreams.create(new StringChunk("", namespace)).collect(ObjectChunk.collector("code")));
		register("datenow", SamStreams.create(new StringChunk("", LocalDateTime.now().toString())).collect(ObjectChunk.collector("code")));
		register("timenow", SamStreams.create(new StringChunk("", Long.toString(System.currentTimeMillis()))).collect(ObjectChunk.collector("code")));
	}
	
	public void loadAbsolute(Path path) {
		logger.verbose("Open file \""+path.getFileName().toString()+"\" at \""+path.toString()+"\"");
		try {
			// Create path
			pathOffset.add(path);
			filename.add(path.getFileName().toString());
			
			// Open file
			File opened=path.toFile();
			InputStream input=new FileInputStream(opened);
			
			//Load
			LanguageDataDeserializer deserializer=new LanguageDataDeserializer(logger, tokenizerConfig::create, this.language);
			ObjectChunk data=deserializer.loadFrom(input);
			DataSerializer serializer=new JsonDataSerializer();
			logger.debug(serializer.write(data));
			compiler.compile(data, this);
			pathOffset.remove(pathOffset.size()-1);
			filename.remove(filename.size()-1);
			logger.verbose("File loaded");
		}catch (FileNotFoundException e) {
			logger.error("Cannot open file \""+path.toString()+"\"");
		}
	}
	
	public void tryLoadAbsolute(Path path) {
		if(!loadedFiles.contains(path.toAbsolutePath().toString())) {
			loadAbsolute(path);
			loadedFiles.add(path.toAbsolutePath().toString());
		}
	}
	
	public void tryLoadRelative(String name) {
		Path loaded=pathOffset.get(pathOffset.size()-1).getParent().resolve(name+".mcsam");
		tryLoadAbsolute(loaded);
	}
	
	public void tryLoadLibrary(String name) {
		tryLoadAbsolute(Path.of(System.getenv().get("MCSAM_PATH")).resolve(name+".mcsam"));
	}
	
	/*public void load(String name) {
		logger.verbose("Open file \""+name+"\"");
		try {
			// Create path
			Path root=pathOffset.get(pathOffset.size()-1).getParent();
			Path openedpath=root.resolve(name+".mcsam");
			pathOffset.add(openedpath);
			filename.add(name);
			
			// Open file
			File opened=openedpath.toFile();
			InputStream input=new FileInputStream(opened);
			LanguageDataDeserializer deserializer=new LanguageDataDeserializer(logger, tokenizerConfig::create, this.language);
			ObjectChunk data=deserializer.loadFrom(input);
			DataSerializer serializer=new JsonDataSerializer();
			logger.debug(serializer.write(data));
			compiler.compile(data, this);
			pathOffset.remove(pathOffset.size()-1);
			filename.remove(filename.size()-1);
			logger.verbose("File loaded");
		}catch (FileNotFoundException e) {
			logger.error("Cannot open file \""+name+"\"");
		}
	}*/
	
	public void enter(String stage) {
		path.add(new PathPart(stage, true));
	}
	
	public void enterHidden(String stage) {
		path.add(new PathPart(stage, false));
	}
	
	public void exit() {
		path.remove(path.size()-1);
	}
	
	public String mcPath() {
		StringBuilder sb=new StringBuilder();
		sb.append(namespace);
		sb.append(":");
		sb.append(SamStreams.create(path).filter(pp->pp.inMcPath).map(pp->pp.name).toString("/"));
		return sb.toString();
	}
	
	public String internalPath() {
		return SamStreams.create(path).map(pp->pp.name).toString("/");
	}
	
	public void register(String name, DataChunk content, List<CommandParameter> arguments) {
		register(name, new SimpleCommandVariable(arguments, content));
	}
	
	public void register(String name, DataChunk content) {
		register(name, new SimpleCommandVariable(Collections.emptyList(), content));
	}
	
	public void register(String name, CommandVariable variable) {
		String internalpath=internalPath();
		variables.put(internalpath+(internalpath.length()>0?"/":"")+name+"@"+variable.getParameters().size(), variable);
		if(name.equals("this"))variables.put(internalPath()+"@"+variable.getParameters().size(), variable);
	}
	
	public String registerFile(String name, Supplier<String> content) {
		enter(name);
		String mcpath=mcPath();
		register("this", SamStreams.create(new StringChunk("", mcpath)).collect(ObjectChunk.collector("code")));
		MCSamFile file=new MCSamFile(mcpath, content.get(), filename.get(filename.size()-1));
		files.put(mcPath(), file);
		exit();
		return mcpath;
	}
	
	public Map<String,MCSamFile> files(){
		return files;
	}
	
	public CommandVariable get(String name, int count) {
		name=name+"@"+count;
		StringBuilder sb=new StringBuilder();
		CommandVariable ret=variables.get(name);
		for(PathPart pathpart : path) {
			sb.append(pathpart.name).append("/");
			sb.append(name);
			CommandVariable nret=variables.get(sb.toString());
			if(nret!=null)ret=nret;
			sb.setLength(sb.length()-name.length());
		}
		return ret;
	}
	
	public Map<String,CommandVariable> variables(){
		return variables;
	}
	
	private class PathPart{
		public String name;
		public boolean inMcPath;
		public PathPart(String name, boolean inMcPath) {
			super();
			this.name = name;
			this.inMcPath = inMcPath;
		}
	}
	
	
	
	public static class MCSamFile{
		public final String name;
		public final String content;
		public final String origin;
		public MCSamFile(String name, String content, String origin) {
			super();
			this.name = name;
			this.content = content;
			this.origin = origin;
		}
	}
}
