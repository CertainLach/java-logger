package pw.lach.logger;

public interface IReceiver {
	void write(LoggerWriteAction action);
}
