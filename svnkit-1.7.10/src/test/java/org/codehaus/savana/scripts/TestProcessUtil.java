package org.codehaus.savana.scripts;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;

public class TestProcessUtil {

    /**
     * Run a process and return any output written to stdout.
     * @throws IOException if the process fails (returns non-zero exit code)
     */
    public static String exec(String... cmdline) throws IOException, InterruptedException {
        // start the process
        Process process = new ProcessBuilder(cmdline).start();

        // close stdin--don't provide any input.
        process.getOutputStream().close();
        // capture stdout and stderr.  assume platform default char encoding.
        CaptureOutput stdout = new CaptureOutput(process.getInputStream());
        CaptureOutput stderr = new CaptureOutput(process.getErrorStream());

        // wait for the process to complete
        int exitCode = process.waitFor();

        // assume a non-zero exit code means the process failed
        if (exitCode != 0) {
            throw new IOException("Failure code " + exitCode + " executing command '" + StringUtils.join(cmdline, ' ') + "': " + stderr.getOutput());
        }
        return stdout.getOutput();
    }

    private static class CaptureOutput extends Thread {
        private final InputStream _in;
        private String _output;

        private CaptureOutput(InputStream in) {
            _in = in;
            start();
        }

        @Override
        public void run() {
            try {
                _output = IOUtils.toString(_in);
            } catch (Throwable t) {
                _output = ExceptionUtils.getStackTrace(t);
            } finally {
                IOUtils.closeQuietly(_in);
            }
        }

        public String getOutput() throws InterruptedException {
            join(); // wait until the run() method has completed and returned
            return _output;
        }
    }
}
