package pw.lach.logger;

import lombok.SneakyThrows;
import pw.lach.logger.receivers.JavaConsoleReceiver;

import java.io.*;
import java.util.*;

@SuppressWarnings("unused")
public class Logger {
	private static int repeatCount;
	private static String lastProvider;
	private static Object lastMessage;
	private static LoggerAction lastType;
	private static Set<IReceiver> receivers = new HashSet<>();
	private String name;
	private Stack<String> identation=new Stack<>();
	private Stack<Long> identationTime = new Stack<>();

	private Map<String,Long> times = new HashMap<>();

	@SuppressWarnings("unused")
	public Logger(String name){
		this.name = name.toUpperCase();
	}

	public Logger(Class c){
		this(c.getSimpleName());
	}

//	@SuppressWarnings("unused")
//	@SneakyThrows
//	public void timeStart(String time){
//		if(this.times.containsKey(time)){
//			throw new Exception(String.format("timeStart(%s) called 2 times with same name!",time));
//		}
//		this.times.put(time,System.currentTimeMillis());
//		LoggerWriteAction loggerWriteAction =new LoggerWriteAction();
//		loggerWriteAction.setType(LoggerAction.TIME_START);
//		loggerWriteAction.setName(time);
//		write(loggerWriteAction);
//	}
//
//	@SuppressWarnings("unused")
//	@SneakyThrows
//	public void timeEnd(String time){
//		if(this.times.containsKey(time)){
//			throw new Exception(String.format("timeEnd(%s) called with unknown name!",time));
//		}
//		LoggerWriteAction loggerWriteAction =new LoggerWriteAction();
//		loggerWriteAction.setType(LoggerAction.TIME_END);
//		loggerWriteAction.setName(time);
//		loggerWriteAction.setTime(System.currentTimeMillis()-this.times.remove(time));
//		write(loggerWriteAction);
//	}

	@SuppressWarnings("unused")
	@SneakyThrows
	public void ident(String name){
		identation.push(name);
		identationTime.push(System.currentTimeMillis());
		LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
		loggerWriteAction.setType(LoggerAction.IDENT);
		loggerWriteAction.setName(name);
		write(loggerWriteAction);
	}

	@SuppressWarnings("unused")
	@SneakyThrows
	public void deent(String assertion){
		if(this.identation.size()==0)
			throw new Exception("deent called with zero length identation stack!");
		LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
		String name = identation.pop();
		assert assertion.equals(name);
		loggerWriteAction.setType(LoggerAction.DEENT);
		loggerWriteAction.setName(name);
		loggerWriteAction.setTime(System.currentTimeMillis()-this.identationTime.pop());
		write(loggerWriteAction);
	}

	@SuppressWarnings({"unused", "WeakerAccess"})
	@SneakyThrows
	public void deent(){
		if(this.identation.size()==0)
			throw new Exception("deent called with zero length identation stack!");
		LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
		String name = identation.pop();
		loggerWriteAction.setType(LoggerAction.DEENT);
		loggerWriteAction.setName(name);
		loggerWriteAction.setTime(System.currentTimeMillis()-this.identationTime.pop());
		write(loggerWriteAction);
	}

	@SuppressWarnings("unused")
	public void deentAll(){
		while(identation.size()>0)
			deent();
	}

	@SuppressWarnings("unused")
	public void info(Object line,Object... params){
		LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
		loggerWriteAction.setType(LoggerAction.INFO);
		loggerWriteAction.setLine(line);
		loggerWriteAction.setParams(params);
		write(loggerWriteAction);
	}

	@SuppressWarnings("unused")
	public void warning(Object line,Object... params){
		LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
		loggerWriteAction.setType(LoggerAction.WARNING);
		loggerWriteAction.setLine(line);
		loggerWriteAction.setParams(params);
		write(loggerWriteAction);
	}

	@SuppressWarnings("unused")
	public void error(Object line,Object... params){
		LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
		loggerWriteAction.setType(LoggerAction.ERROR);
		loggerWriteAction.setLine(line);
		loggerWriteAction.setParams(params);
		write(loggerWriteAction);
	}

	@SuppressWarnings("unused")
	public void debug(Object line,Object... params){
		LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
		loggerWriteAction.setType(LoggerAction.DEBUG);
		loggerWriteAction.setLine(line);
		loggerWriteAction.setParams(params);
		write(loggerWriteAction);
	}

//	private Set<String> progresses=new HashSet<>();
//
//	@SuppressWarnings("unused")
//	@SneakyThrows
//	public void progress(String name,boolean progress){
//		if(progress){
//			if(progresses.contains(name))
//				throw new Exception("Progress is already started!");
//			progresses.add(name);
//			LoggerWriteAction loggerWriteAction =new LoggerWriteAction();
//			loggerWriteAction.setType(LoggerAction.PROGRESS_START);
//			loggerWriteAction.setName(name);
//			write(loggerWriteAction);
//		}else{
//			if(!progresses.contains(name))
//				throw new Exception("Progress is not started!");
//			progresses.remove(name);
//			LoggerWriteAction loggerWriteAction =new LoggerWriteAction();
//			loggerWriteAction.setType(LoggerAction.PROGRESS_END);
//			loggerWriteAction.setName(name);
//			write(loggerWriteAction);
//		}
//	}
//
//	@SuppressWarnings("unused")
//	@SneakyThrows
//	public void progress(String name,int progress,Object info){
//		if(!progresses.contains(name))
//			throw new Exception("Progress is not started!");
//		LoggerWriteAction loggerWriteAction =new LoggerWriteAction();
//		loggerWriteAction.setType(LoggerAction.PROGRESS);
//		loggerWriteAction.setName(name);
//		loggerWriteAction.setProgress(progress);
//		loggerWriteAction.setLine(info);
//		write(loggerWriteAction);
//	}

	private void write(LoggerWriteAction loggerWriteAction){
		loggerWriteAction.writtenAt=System.currentTimeMillis();
		loggerWriteAction.writtenFrom=name;
		loggerWriteAction.identationLength=identation.size();
		_write(loggerWriteAction);
	}
	private static boolean noReceiversWarned = false;
	private static void _write(LoggerWriteAction loggerWriteAction){
		if(receivers.size()==0) {
			if(!noReceiversWarned) {
				PrintStream ps = new PrintStream(new FileOutputStream(FileDescriptor.err));
				ps.println("No receivers are defined for logger! See docs for info about this!");
				noReceiversWarned=true;
			}
			return;
		}
		if(isRepeating(loggerWriteAction.writtenFrom, loggerWriteAction.line, loggerWriteAction.type))
			repeatCount++;
		else
			resetRepeating(loggerWriteAction.writtenFrom, loggerWriteAction.line, loggerWriteAction.type);
		if(!LoggerAction.isRepeatable(loggerWriteAction.type))
			loggerWriteAction.repeats=repeatCount;
		loggerWriteAction.repeated= loggerWriteAction.repeats>0;
		receivers.forEach(receiver-> receiver.write(loggerWriteAction));
	}

	private static void resetRepeating(String lastProvider, Object lastMessage, LoggerAction lastType){
		Logger.lastProvider=lastProvider;
		Logger.lastMessage=lastMessage;
		Logger.lastType=lastType;
		repeatCount=0;
	}

	private static boolean isRepeating(String lastProvider, Object lastMessage, LoggerAction lastType){
		if(Logger.lastProvider==null|| Logger.lastMessage==null)
			return false;
		return Logger.lastProvider.equals(lastProvider)&& Logger.lastMessage.equals(lastMessage)&& Logger.lastType==lastType;
	}

	@SuppressWarnings("unused")
	public static void addReceiver(IReceiver receiver){
		receivers.add(receiver);
	}

	public static void main(String[] argv){
		Logger.addReceiver(new JavaConsoleReceiver());
		Logger logger = new Logger(Logger.class);
		logger.info("Test");
	}
}


































