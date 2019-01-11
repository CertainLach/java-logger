package pw.lach.logger;

import lombok.SneakyThrows;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;


public final class Logger {
    private static int repeatCount;
    private static String lastProvider;
    private static Object lastMessage;
    private static LoggerAction lastType;
    private static final Set<IReceiver> receivers = new HashSet<>();
    private String name;
    private final Stack<String> identation = new Stack<>();
    private final Stack<Long> identationTime = new Stack<>();

    public Logger(String name) {
        this.name = name.toUpperCase();
    }

    @SneakyThrows
    public void ident(String name) {
        identation.push(name);
        identationTime.push(System.currentTimeMillis());
        LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
        loggerWriteAction.setType(LoggerAction.IDENT);
        loggerWriteAction.setName(name);
        write(loggerWriteAction);
    }

    public void deent() {
        if (this.identation.isEmpty()) {
            throw new RuntimeException("deent called with zero length identation stack!");
        } LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
        String name = identation.pop();
        loggerWriteAction.setType(LoggerAction.DEENT);
        loggerWriteAction.setName(name);
        loggerWriteAction.setTime(System.currentTimeMillis() - this.identationTime.pop());
        write(loggerWriteAction);
    }

    public void deentAll() {
        while (!identation.isEmpty()) {
            deent();
        }
    }

    public void info(Object line, Object... params) {
        LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
        loggerWriteAction.setType(LoggerAction.INFO);
        loggerWriteAction.setLine(line);
        loggerWriteAction.setParams(params);
        write(loggerWriteAction);
    }


    public void warning(Object line, Object... params) {
        LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
        loggerWriteAction.setType(LoggerAction.WARNING);
        loggerWriteAction.setLine(line);
        loggerWriteAction.setParams(params);
        write(loggerWriteAction);
    }


    public void error(Object line, Object... params) {
        LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
        loggerWriteAction.setType(LoggerAction.ERROR);
        loggerWriteAction.setLine(line);
        loggerWriteAction.setParams(params);
        write(loggerWriteAction);
    }


    public void debug(Object line, Object... params) {
        LoggerWriteAction loggerWriteAction = new LoggerWriteAction();
        loggerWriteAction.setType(LoggerAction.DEBUG);
        loggerWriteAction.setLine(line);
        loggerWriteAction.setParams(params);
        write(loggerWriteAction);
    }

    private void write(LoggerWriteAction loggerWriteAction) {
        loggerWriteAction.writtenAt = System.currentTimeMillis();
        loggerWriteAction.writtenFrom = name;
        loggerWriteAction.identationLength = identation.size();
        _write(loggerWriteAction);
    }

    private static boolean noReceiversWarned = false;

    private static void _write(LoggerWriteAction loggerWriteAction) {
        if (receivers.isEmpty()) {
            if (!noReceiversWarned) {
                System.err.println("No receivers are defined for logger! See docs for info about this!");
                noReceiversWarned = true;
            }
            return;
        }
        if (isRepeating(loggerWriteAction.writtenFrom, loggerWriteAction.line, loggerWriteAction.type))
            repeatCount++;
        else
            resetRepeating(loggerWriteAction.writtenFrom, loggerWriteAction.line, loggerWriteAction.type);
        if (!LoggerAction.isRepeatable(loggerWriteAction.type))
            loggerWriteAction.repeats = repeatCount;
        loggerWriteAction.repeated = loggerWriteAction.repeats > 0;
        receivers.forEach(receiver -> receiver.write(loggerWriteAction));
    }

    private static void resetRepeating(String lastProvider, Object lastMessage, LoggerAction lastType) {
        Logger.lastProvider = lastProvider;
        Logger.lastMessage = lastMessage;
        Logger.lastType = lastType;
        repeatCount = 0;
    }

    private static boolean isRepeating(String lastProvider, Object lastMessage, LoggerAction lastType) {
        if (Logger.lastProvider == null || Logger.lastMessage == null)
            return false;
        return Logger.lastProvider.equals(lastProvider) && Logger.lastMessage.equals(lastMessage) && Logger.lastType == lastType;
    }

    public static void addReceiver(IReceiver receiver) {
        receivers.add(receiver);
    }
}
