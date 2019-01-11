package pw.lach.logger;

import lombok.Data;

@Data
public class LoggerWriteAction {
    LoggerAction type;
    String name;
    long time;
    Object line;
    Object[] params;
    int progress;

    long writtenAt;
    String writtenFrom;
    int identationLength;

    int repeats = 0;
    boolean repeated;
}
