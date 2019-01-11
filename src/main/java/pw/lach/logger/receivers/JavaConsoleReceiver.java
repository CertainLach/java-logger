package pw.lach.logger.receivers;

import com.google.gson.Gson;
import pw.lach.logger.IReceiver;
import pw.lach.logger.LoggerWriteAction;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;

public final class JavaConsoleReceiver implements IReceiver {

    private int nameLimit;
    private final Gson gson = new Gson();
    private final Map<Object, String> weakCache = new WeakHashMap<>();
    private static final BufferedWriter bw;

    static {
        bw = new BufferedWriter(new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(FileDescriptor.out), 8192), StandardCharsets.UTF_8), 8192);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                bw.flush();
            } catch (IOException ignored) { }
        }, "JavaConsoleReceiver flush thread"));
    }

    private String stringifyObject(Object object) {
        if (object == null)
            return "null";
        String cache = weakCache.get(object);
        if (cache == null) {
            if (object instanceof Throwable) {
                StackTraceElement[] stack = ((Throwable) object).getStackTrace();
                StringBuilder exception = new StringBuilder();
                for (StackTraceElement s : stack) {
                    exception.append(s.toString()).append("\n\t");
                }
                cache = exception.toString();
            } else if (object instanceof String)
                cache = (String) object;
            else if (object instanceof Integer || object instanceof Long || object instanceof Byte || object instanceof Float || object instanceof Short)
                cache = String.valueOf(object);
            else if (object instanceof BigInteger)
                cache = object.toString();
            else if (object instanceof Date)
                cache = object.toString();
            else cache = gson.toJson(object);
            weakCache.put(object, cache);
        }
        return cache;
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

    private String stringifyName(String provider, String escapeCode) {
        return "\u001B[" + escapeCode + "\u001B[1m" + (nameLimit == 0 ? "" : padStart(provider, nameLimit, ' ')) +
                "\u001B[0m";
    }

    String stringifyName(String provider) {
        return stringifyName(provider, "35m");
    }

    String stringifyIdentData(String provider, LoggerWriteAction data) {
        return " " + stringifyName(provider) + " \u001B[35m" + stringiftIdent(data.getIdentationLength() - 1, '>') +
                "\u001B[1m " + data.getName() + "\u001B[0m\n";
    }

    String stringifyDeentData(String provider, LoggerWriteAction data) {
        return " " + stringifyName(provider) + " \u001B[35m" + stringiftIdent(data.getIdentationLength(), '<') +
                "\u001B[1m " + data.getName() + "\u001B[22m (Done in " + data.getTime() + "ms)\u001B[0m\n";
    }

    String stringifyCommonData(String escapeCode, String provider, LoggerWriteAction data, String outString) {
        int i = 0;
        StringBuilder ret = new StringBuilder();
        for (String s : outString.split("\n")) {
            if (i == 0) {
                ret.append(" \u001B[0m").append(stringifyName(provider, escapeCode)).append("\u001B[0m").append(stringifyIdent(data.getIdentationLength())).append(s).append("\n");
                i = 1;
            } else {
                ret.append(" \u001B[0m").append(stringifyName("|", escapeCode)).append("\u001B[0m").append(stringifyIdent(data.getIdentationLength())).append(s).append("\n");
            }
        }
        return ret.toString();
    }

    private void writeInfoData(String provider, LoggerWriteAction data, String outString) {
        writeStdout(stringifyCommonData("34m", provider, data, outString));
    }

    private void writeErrorData(String provider, LoggerWriteAction data, String outString) {
        writeStdout(stringifyCommonData("31m", provider, data, outString));
    }

    private void writeWarningData(String provider, LoggerWriteAction data, String outString) {
        writeStdout(stringifyCommonData("33m", provider, data, outString));
    }

    private void writeDebugData(String provider, LoggerWriteAction data, String outString) {
        writeStdout(stringifyCommonData("90m", provider, data, outString));
    }

    private void writeStdout(String string) {
        try {
            bw.write(string);
        } catch (IOException e) {
            throw new InternalError(e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public JavaConsoleReceiver(int nameLimit) {
        this.nameLimit = nameLimit;
    }

    public JavaConsoleReceiver() {
        this(15);
    }

    @Override
    public void write(LoggerWriteAction data) {
        String outString = stringifyData(data);
        switch (data.getType()) {
            case IDENT:
                writeStdout(stringifyIdentData(data.getWrittenFrom(), data));
                break;
            case DEENT:
                writeStdout(stringifyDeentData(data.getWrittenFrom(), data));
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
                if (System.getenv("DEBUG") != null && (System.getenv("DEBUG").contains(data.getWrittenFrom()) || System.getenv("DEBUG").equals("*")))
                    writeDebugData(data.getWrittenFrom(), data, outString);
                break;
        }
    }
}

































