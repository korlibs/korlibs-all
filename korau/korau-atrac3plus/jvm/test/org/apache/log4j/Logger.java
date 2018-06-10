package org.apache.log4j;

public class Logger {
    public final String name;

    public Logger(final String name) {
        this.name = name;
    }

    public static Logger getLogger(String name) {
        return new Logger(name);
    }

    public boolean isDebugEnabled() {
        //return true;
        return false;
    }

    public void error(String msg) {
        System.err.println(msg);
    }

    public void warn(String msg) {
        System.err.println(msg);
    }

    public void debug(String msg) {
        //System.err.println(msg);
    }
}
