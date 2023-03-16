package jempasam.ccompiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


import jempasam.ccompiler.CommandContext.MCSamFile;
import jempasam.ccompiler.element.MCSamCode;
import jempasam.commandline.CommandLineParser;
import jempasam.commandline.CommandLines;
import jempasam.commandline.option.Option;
import jempasam.data.chunk.ObjectChunk;
import jempasam.data.chunk.value.StringChunk;
import jempasam.data.deserializer.DataDeserializer;
import jempasam.data.deserializer.DataDeserializers;
import jempasam.data.deserializer.language.LanguageDataDeserializer;
import jempasam.data.deserializer.language.SimpleLanguageLoader;
import jempasam.data.deserializer.language.TokenType;
import jempasam.data.serializer.DataSerializer;
import jempasam.data.serializer.JsonDataSerializer;
import jempasam.logger.SLogger;
import jempasam.logger.SLoggers;
import jempasam.logger.VisibilitySLogger;
import jempasam.map.MultiMap;
import jempasam.samstream.SamStreams;
import jempasam.samstream.collectors.SamCollectors;
import jempasam.samstream.text.TokenizerConfig;

public class CCMain {
	
	private static SLogger logger;
	private static SLogger langLogger;
	private static SLogger deserLogger;
	private static SLogger resultLogger;
	
	public static void main(String[] args) {
		logger=new VisibilitySLogger(SLoggers.OUT, 100);
		
		// Create parser
		CommandLineParser parser=new CommandLineParser(logger);
		Option<Integer> argVisibility=parser.addOption("v", "visibility","log visibility" , CommandLines.INTEGER);
		Option<Boolean> langVisibility=parser.addOption("L", "languageLogger","enable language deserializer logger" , CommandLines.BOOLEAN);
		Option<Boolean> deserVisibility=parser.addOption("D", "deserializerLogger","enable code deserializer logger" , CommandLines.BOOLEAN);
		Option<Boolean> resultVisibility=parser.addOption("R", "resultLogger","enable code result" , CommandLines.BOOLEAN);
		parser.parse(args);
		
		// Loggers
		logger=new VisibilitySLogger(SLoggers.OUT, parser.getValue(argVisibility).orElse(500));
		langLogger=parser.getValue(langVisibility).isPresent() ? logger : SLoggers.VOID;
		deserLogger=parser.getValue(deserVisibility).isPresent() ? logger : SLoggers.VOID;
		resultLogger=parser.getValue(resultVisibility).isPresent() ? logger : SLoggers.VOID;
		
		// Run
		if(parser.arguments().size()==4 && parser.arguments().get(0).equals("project")) {
			String srcdir=parser.arguments().get(1);
			String bindir=parser.arguments().get(2);
			String namespace=parser.arguments().get(3);
			whole(srcdir, bindir, namespace);
		}
		else if(parser.arguments().size()==2 && parser.arguments().get(0).equals("deserialize")) {
			String srcdir=parser.arguments().get(1);
			deserialize(srcdir);
		}
		else if(parser.arguments().size()==3 && parser.arguments().get(0).equals("make")) {
			String makefile=parser.arguments().get(1);
			String target=parser.arguments().get(2);
			make(makefile, target);
		}
		else {
			logger.info("project <srctarget> <bindir> <namespace>");
			logger.info("make <makefile> <target>");
			logger.info(parser.getDoc());
		}
	}
	
	public static void deserialize(String srcfile) {
		DataDeserializer languageDeserializer=DataDeserializers.createJSONLikeStrobjoDS(langLogger);
		SimpleLanguageLoader  languageLoader=new SimpleLanguageLoader(langLogger);
		InputStream input=CCMain.class.getClassLoader().getResourceAsStream("jempasam/language.swjson");
		ObjectChunk languageData=languageDeserializer.loadFrom(input);
		languageData.childStream().objects().forEach(d->d.childStream().objects().forEach(oc->oc.add(new StringChunk("name", oc.getName()))));
		languageLoader.load(languageData);
		langLogger.debug(SamStreams.create(languageLoader.tokenTypes()).mapToString().collect(SamCollectors.concatenate("\n")));
		TokenType language=languageLoader.tokenTypes().get("filestart");
		
		TokenizerConfig tokenizerConfig=new TokenizerConfig().setCommenter("#").setSpliter("\t\r ").setAlone("{}[]()=.,:\"'\n").setFirstOfToken("<").setLastOfToken(">").setGroupAlone("&%;");
		LanguageDataDeserializer deserializer=new LanguageDataDeserializer(SLoggers.OUT, tokenizerConfig::create, language);
		
		DataSerializer serializer=new JsonDataSerializer();
		
		try {
			File inputfile=Path.of(srcfile).toFile();
			ObjectChunk inputdata;
			inputdata = deserializer.loadFrom(new FileInputStream(inputfile));
			System.out.println(serializer.write(inputdata));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void whole(String srcdir, String bindir, String namespace) {		
		// Paths
		Path inputPath=Path.of(srcdir);
		Path inputDirPath=inputPath.getParent();
		String inputFileName=inputPath.getFileName().toString();
		Path outputPath=Path.of(".").resolve(bindir);
		
		if(!inputDirPath.toFile().isDirectory())logger.error("No such input directory \""+inputDirPath+"\"");
		if(!outputPath.getParent().toFile().isDirectory())logger.error("No such output directory \""+outputPath+"\"");
		
		File outputDir=outputPath.toFile();
		outputDir.mkdir();
		
		// Compile
		CommandContext context=new CommandContext(namespace,inputDirPath,langLogger,deserLogger);
		context.tryLoadRelative(inputFileName);
		resultLogger.verbose(SamStreams.create(context.files().values()).map(e->"["+e.name+"|"+e.origin+"]\n"+e.content.replace(System.lineSeparator(), "|")).toString("\n\n")+"\n");
		resultLogger.verbose(context.variables().entrySet().stream().map(e->"("+e.getKey()+"):\n"+e.getValue()).collect(Collectors.joining("\n\n")));
		
		// Save
		for(MCSamFile file : context.files().values()) {
			try {
				MCSamCode.saveFile(outputPath, file);
			} catch (IOException e1) { e1.printStackTrace(); }
		}
		// Tools
		/*SLogger logger=SLoggers.OUT;
		DataSerializer serializer=new JsonDataSerializer();
		
		Map<String, String> files=new HashMap<>();
		CommandContext context=new CommandContext("wand",Path.of("inputcode"),logger);
		context.load("commandTest");
		System.out.println(SamStreams.create(context.files.entrySet()).map(e->"["+e.getKey()+"]\n"+e.getValue().replace(System.lineSeparator(), "|")).toString("\n\n")+"\n");
		System.out.println(context.variables().entrySet().stream().map(e->"("+e.getKey()+"):\n"+e.getValue()).collect(Collectors.joining("\n\n")));*/
	}
	
	public static void make(String makefile, String target) {
		try {
			// Read Data
			DataDeserializer des=DataDeserializers.createYAMLLikeChardentDS(SLoggers.VOID);
			Path makefilePath=Path.of(".",makefile);
			FileInputStream input=new FileInputStream(makefilePath.toFile());
			ObjectChunk data=des.loadFrom(input);
			
			Path parent=makefilePath.getParent();
			Path srcpath=Optional.ofNullable(data.getValue(String.class, "src")).map(parent::resolve).orElseThrow(()->new NoSuchElementException("Miss src path"));
			Path binpath=Optional.ofNullable(data.getValue(String.class, "bin")).map(parent::resolve).orElseThrow(()->new NoSuchElementException("Miss bin path"));
			String namespace=Optional.ofNullable(data.getValue(String.class, "namespace")).orElseThrow(()->new NoSuchElementException("Miss namespace"));
			
			data.removeAll("src","bin","namespace");
			
			data.childStream().valuesOfType(String.class).filter(strc->strc.getName().equals("")).forEach(strc->{
				strc.setName(strc.getValue());
				strc.setValue("");
			});
			MultiMap<String, String> dependencies=new MultiMap<>(new HashMap<>(), ArrayList::new);
			data.childStream().valuesOfType(String.class).filter(strc->strc.getValue().length()>0).forEach(strc->dependencies.add(strc.getValue(), strc.getName()));
			
			logger.verbose(new JsonDataSerializer().write(data));
			logger.verbose("From \""+srcpath+"\" to \""+binpath+"\" as \""+namespace+"\"");
			
			// Load
			CommandContext context=new CommandContext(namespace,srcpath,langLogger,deserLogger);
			Set<String> loaded=new HashSet<>();
			List<String> toloads=List.of(target);
			while(toloads.size()>0) {
				List<String> newtoload=new ArrayList<>();
				for(String toload : toloads) {
					newtoload.addAll(dependencies.get(toload));
					loaded.add(toload);
					context.tryLoadRelative(toload);
					resultLogger.verbose(SamStreams.create(context.files().values()).map(e->"["+e.name+"|"+e.origin+"]\n"+e.content.replace(System.lineSeparator(), "|")).toString("\n")+"\n");
					resultLogger.verbose(context.variables().entrySet().stream().map(e->"("+e.getKey()+"):\n"+e.getValue()).collect(Collectors.joining("\n")));
				}
				toloads=newtoload;
			}
			
			// Save
			for(MCSamFile file : context.files().values()) {
				try {
					if(loaded.contains(file.origin)) {
						MCSamCode.saveFile(binpath, file);
						logger.verbose("File \""+file.name+"\" saved!");
					}
				} catch (IOException e1) { e1.printStackTrace(); }
			}
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
		} catch (NoSuchElementException e) {
			logger.error(e.getMessage());
		}
	}

}
