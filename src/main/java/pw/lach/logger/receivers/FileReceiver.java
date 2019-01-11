package pw.lach.logger.receivers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pw.lach.logger.IReceiver;
import pw.lach.logger.LoggerWriteAction;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public class FileReceiver implements IReceiver {

	private static final Pattern LINE_PATTERN = Pattern.compile("\n");
	private static final Pattern OWNER_PATTERN = Pattern.compile("[^a-zA-Z1-9]");
	private static final String ls = System.lineSeparator();
	private static final Object logLock = new Object();

	private static final Format dayDateFormat = new SimpleDateFormat("yy-MM-dd");
	private static final Format timeDateFormat = new SimpleDateFormat("hh-mm-ss.SSS");
	private final Format dateFormat = new SimpleDateFormat("MM-dd hh:mm");

	private File logStorageFolder;

	public FileReceiver(File logStorageFolder) {
		// this.logStorageFolder=logStorageFolder;
		mkdirDirectory(logStorageFolder);
		Date date = new Date();
		File mainFolder = new File(logStorageFolder, dayDateFormat.format(date));
		mkdirDirectory(mainFolder);
		File subFolder = new File(mainFolder, timeDateFormat.format(date));
		mkdirDirectory(subFolder);
		this.logStorageFolder = subFolder;
	}

	private Gson gson = new GsonBuilder().create();

	private String stringifyObject(Object object) {
		if (object == null)
			return "null";
		if (object instanceof Throwable) {
			final StringBuilder stringBuilder = new StringBuilder();
			Throwable throwable = (Throwable) object;
			stringBuilder.append("Exception (").append(throwable.getClass().getName()).append("): ");
			String message;
			if (throwable.getSuppressed().length >= 1) {
				message = (throwable.getSuppressed()[0].getMessage());
			} else {
				message = (throwable.getMessage());
			}
			if (message != null) {
				stringBuilder.append(message)
						.append(':');
			}
			for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
				stringBuilder.append("\n\t").append(stackTraceElement.getClassName()).append('#')
						.append(stackTraceElement.getMethodName());
				if (stackTraceElement.getFileName() != null) {
					stringBuilder.append(" (")
							.append(stackTraceElement.getFileName()).append(':')
							.append(stackTraceElement.getLineNumber())
							.append(')');
				}
			}
			if (throwable.getCause() != null) {
				stringBuilder.append("\n")
						.append(Arrays.stream(LINE_PATTERN.split(stringifyObject(throwable.getCause()), -1)).map(e -> "\t" + e)
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

	private String stringifyIdentData(String provider, LoggerWriteAction data) {
		return '[' + dateFormat.format(new Date(data.getWrittenAt())) + "|       ] " + " "
				+ stringiftIdent(data.getIdentationLength() - 1, '>') + " " + data.getName() + ls;
	}

	private String stringifyDeentData(String provider, LoggerWriteAction data) {
		return "[" + dateFormat.format(new Date(data.getWrittenAt())) + "|       ] " + " "
				+ stringiftIdent(data.getIdentationLength(), '<') + " " + data.getName() + " (Done in " + data.getTime()
				+ "ms)" + ls;
	}

	private String stringifyCommonData(String type, String provider, LoggerWriteAction data, String outString) {
		int i = 0;
		StringBuilder ret = new StringBuilder();
		String date = dateFormat.format(new Date(data.getWrittenAt()));
		for (String s : LINE_PATTERN.split(outString, -1)) {
			if (i == 0) {
				ret.append('[').append(date).append('|').append(type)
						.append("] ").append(stringifyIdent(data.getIdentationLength())).append(s)
						.append(ls);
				i = 1;
			} else {
				ret.append("                      ").append(stringifyIdent(data.getIdentationLength())).append(s)
						.append(ls);
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
		owner = OWNER_PATTERN.matcher(owner).replaceAll("_");
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

	private static String createFailureMessage(File file) {
		return "Could not mkdir: " + file.getAbsolutePath();
	}

	private static void mkdirDirectory(File folder) {
		if (!folder.isDirectory() && !folder.mkdirs()) {
			throw new RuntimeException(createFailureMessage(folder));
		}
	}
}
