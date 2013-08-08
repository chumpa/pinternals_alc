package com.pinternals.alc;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

public final class LogFormatter extends Formatter {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static SimpleDateFormat fmt = null;
    
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        if (fmt==null) {
        	String p = LogManager.getLogManager().getProperty("com.pinternals.alc.LogFormatter.dateFormat");
        	if (p==null) p = "yyyy-MM-dd HH:mm:ss";
        	fmt = new SimpleDateFormat(p);
        }
        String d = fmt.format(new Date(record.getMillis()));
        
        sb.append(d)
            .append(" ")
            .append(record.getLevel().getLocalizedName())
            .append(": ")
            .append(formatMessage(record))
            .append(LINE_SEPARATOR);

//        if (record.getThrown() != null) {
//            try {
////                StringWriter sw = new StringWriter();
////                PrintWriter pw = new PrintWriter(sw);
////                record.getThrown().printStackTrace(pw);
////                pw.close();
////                sb.append(sw.toString());
////            	sb.append(record.getThrown().getMessage());
//            } catch (Exception ex) {
//                // ignore
//            }
//        }

        return sb.toString();
    }
}