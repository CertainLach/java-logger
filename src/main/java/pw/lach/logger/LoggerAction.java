package pw.lach.logger;

public enum LoggerAction {
    IDENT,
    DEENT,

    INFO, WARNING, ERROR,

    DEBUG;

    static boolean isRepeatable(LoggerAction loggerAction) {
        return loggerAction == IDENT || loggerAction == DEENT;
    }
}
