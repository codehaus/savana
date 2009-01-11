package org.codehaus.savana.scripts;

/**
 */
public class SavanaScriptsTestException extends RuntimeException {

    private final String _out;
    private final String _err;

    public SavanaScriptsTestException(String out, String err) {
        super((out + err).trim());
        _out = out;
        _err = err;
    }

    public String getOut() {
        return _out;
    }

    public String getErr() {
        return _err;
    }
}
