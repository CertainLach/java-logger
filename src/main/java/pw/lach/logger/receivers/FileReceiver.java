package pw.lach.logger.receivers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pw.lach.logger.IReceiver;
import pw.lach.logger.LoggerWriteAction;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public class FileReceiver implements IReceiver {
	private static final Object logLock = new Object();
	private int nameLimit;
	private File logStorageFolder;

	@SuppressWarnings("WeakerAccess")
	public FileReceiver(File logStorageFolder, int nameLimit) {
		this.nameLimit = nameLimit;
		// this.logStorageFolder=logStorageFolder;
		logStorageFolder.mkdirs();
		Date date = new Date();
		File mainFolder = new File(logStorageFolder, dayDateFormat.format(date));
		mainFolder.mkdir();
		File subFolder = new File(mainFolder, timeDateFormat.format(date));
		subFolder.mkdir();
		this.logStorageFolder = subFolder;
	}

	public FileReceiver(File logStorageFolder) {
		this(logStorageFolder, 15);
	}

	private Gson gson = new GsonBuilder().create();

	private String stringifyObject(Object object) {
		if (object == null)
			return "null";
		if (object instanceof Throwable) {
			final StringBuilder stringBuilder = new StringBuilder();
			Throwable throwable = (Throwable) object;
			stringBuilder.append("Exception (").append(throwable.getClass().getName()).append("): ");
			int trimCount = -1;
			String message = null;
			if (throwable.getSuppressed().length >= 1) {
				message = (throwable.getSuppressed()[0].getMessage());
			} else {
				message = (throwable.getMessage());
			}
			if (message != null) {
				stringBuilder.append(message);
				stringBuilder.append(":");
			}
			for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
				stringBuilder.append("\n\t").append(stackTraceElement.getClassName()).append("#")
						.append(stackTraceElement.getMethodName());
				if (stackTraceElement.getFileName() != null) {
					stringBuilder.append(" (");
					stringBuilder.append(stackTraceElement.getFileName()).append(":");
					stringBuilder.append(stackTraceElement.getLineNumber());
					stringBuilder.append(")");
				}
			}
			if (throwable.getCause() != null) {
				stringBuilder.append("\n");
				stringBuilder.append(Arrays.stream(stringifyObject(throwable.getCause()).split("\n")).map(e -> "\t" + e)
						.collect(Collectors.joining("\n")).replaceFirst("Exception", "Caused by"));
			}
			return stringBuilder.toString();
		}
		if (object instanceof String)
			return (String) object;
		if (object instanceof Integer || object instanceof Long || object instanceof Byte || object instanceof Float
				|| object instanceof Short)
			return String.valueOf(object);
		if (object instanceof BigInteger)
			return object.toString();
		if (object instanceof Date)
			return object.toString();
		return gson.toJson(object);
	}

	private String stringifyData(LoggerWriteAction data) {
		StringBuilder right = new StringBuilder(stringifyObject(data.getLine()));
		if (data.getParams() != null) {
			for (Object o : data.getParams()) {
				right.append(" ").append(stringifyObject(o));
			}
		}
		return right.toString();
	}

	private String padStart(String string, int nameLimit, char padder) {
		StringBuilder stringBuilder = new StringBuilder(string);
		while (stringBuilder.length() < nameLimit)
			stringBuilder.insert(0, padder);
		string = stringBuilder.toString();
		return string;
	}

	private String stringiftIdent(int count, char symbolNeeded) {
		return String.join("", Collections.nCopies(count, "  ")) + symbolNeeded;
	}

	private String stringifyIdent(int count) {
		return stringiftIdent(count, ' ');
	}

	String stringifyIdentData(String provider, LoggerWriteAction data) {
		return "[" + dateFormat.format(new Date(data.getWrittenAt())) + "|       ] " + " "
				+ stringiftIdent(data.getIdentationLength() - 1, '>') + " " + data.getName() + System.lineSeparator();
	}

	String stringifyDeentData(String provider, LoggerWriteAction data) {
		return "[" + dateFormat.format(new Date(data.getWrittenAt())) + "|       ] " + " "
				+ stringiftIdent(data.getIdentationLength(), '<') + " " + data.getName() + " (Done in " + data.getTime()
				+ "ms)" + System.lineSeparator();
	}

	SimpleDateFormat dayDateFormat = new SimpleDateFormat("yy-MM-dd");
	SimpleDateFormat timeDateFormat = new SimpleDateFormat("hh-mm-ss.SSS");

	SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd hh:mm");

	String stringifyCommonData(String type, String provider, LoggerWriteAction data, String outString) {
		int i = 0;
		StringBuilder ret = new StringBuilder();
		// noinspection ReplaceAllDot
		for (String s : outString.split("\n")) {
			if (i == 0) {
				ret.append("[").append(dateFormat.format(new Date(data.getWrittenAt()))).append("|").append(type)
						.append("] ").append(stringifyIdent(data.getIdentationLength())).append(s)
						.append(System.lineSeparator());
				i = 1;
			} else {
				ret.append("                      ").append(stringifyIdent(data.getIdentationLength())).append(s)
						.append(System.lineSeparator());
			}
		}
		return ret.toString();
	}

	private void writeInfoData(String provider, LoggerWriteAction data, String outString) {
		writeStdout(provider, stringifyCommonData("INFO   ", provider, data, outString));
	}

	private void writeErrorData(String provider, LoggerWriteAction data, String outString) {
		writeStdout(provider, stringifyCommonData("ERROR  ", provider, data, outString));
	}

	private void writeWarningData(String provider, LoggerWriteAction data, String outString) {
		writeStdout(provider, stringifyCommonData("WARNING", provider, data, outString));
	}

	private void writeDebugData(String provider, LoggerWriteAction data, String outString) {
		writeStdout(provider, stringifyCommonData("DEBUG  ", provider, data, outString));
	}

	// private PrintStream ps = new PrintStream(new
	// FileOutputStream(FileDescriptor.out));
	private void writeStdout(String owner, String string) {
		owner = owner.replaceAll("[^a-zA-Z1-9]", "_");
		// ps.print(string);
		try {
			Files.write(new File(logStorageFolder, owner + ".log").toPath(), (string).getBytes(),
					StandardOpenOption.APPEND);
		} catch (Exception e) {
			try {
				new File(logStorageFolder, owner + ".log").createNewFile();
				writeStdout(owner, string);
			} catch (IOException e1) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void write(LoggerWriteAction data) {
		synchronized (logLock) {
			String outString = stringifyData(data);
			switch (data.getType()) {
			case IDENT:
				writeStdout(data.getWrittenFrom(), stringifyIdentData(data.getWrittenFrom(), data));
				break;
			case DEENT:
				writeStdout(data.getWrittenFrom(), stringifyDeentData(data.getWrittenFrom(), data));
				break;
			case INFO:
				writeInfoData(data.getWrittenFrom(), data, outString);
				break;
			case ERROR:
				writeErrorData(data.getWrittenFrom(), data, outString);
				break;
			case WARNING:
				writeWarningData(data.getWrittenFrom(), data, outString);
				break;
			case DEBUG:
				if (System.getenv("DEBUG") != null && (System.getenv("DEBUG").contains(data.getWrittenFrom())
						|| System.getenv("DEBUG").equals("*")))
					writeDebugData(data.getWrittenFrom(), data, outString);
				break;
			}
		}
	}
}
