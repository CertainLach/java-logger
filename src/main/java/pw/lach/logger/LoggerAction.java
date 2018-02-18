package pw.lach.logger;

public enum LoggerAction {
	IDENT,
	DEENT,

	INFO, WARNING, ERROR,

	DEBUG;

//	TIME_START,
//	TIME_END,

//	PROGRESS,
//	PROGRESS_START,
//	PROGRESS_END;

	static boolean isRepeatable(LoggerAction loggerAction){
		return loggerAction == IDENT || loggerAction == DEENT;// || loggerAction == TIME_START || loggerAction == TIME_END || loggerAction == PROGRESS || loggerAction == PROGRESS_START || loggerAction == PROGRESS_END;
	}
}
