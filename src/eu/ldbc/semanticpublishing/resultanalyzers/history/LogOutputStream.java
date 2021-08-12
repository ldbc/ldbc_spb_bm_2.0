package eu.ldbc.semanticpublishing.resultanalyzers.history;

import org.slf4j.Logger;
import java.io.OutputStream;

public class LogOutputStream extends OutputStream {
	/** The logger where to log the written bytes. */
	private Logger logger;

	/** The internal memory for the written bytes. */
	private String mem;

	/**
	 * Creates a new log output stream which logs bytes to the specified logger with the specified
	 * level.
	 *
	 * @param logger the logger where to log the written bytes
	 */
	public LogOutputStream (Logger logger) {
		setLogger (logger);
		mem = "";
	}

	/**
	 * Sets the logger where to log the bytes.
	 *
	 * @param logger the logger
	 */
	public void setLogger (Logger logger) {
		this.logger = logger;
	}

	/**
	 * Returns the logger.
	 *
	 * @return DOCUMENT ME!
	 */
	public Logger getLogger () {
		return logger;
	}

	/**
	 * Writes a byte to the output stream. This method flushes automatically at the end of a line.
	 *
	 * @param b DOCUMENT ME!
	 */
	public void write (int b) {
		byte[] bytes = new byte[1];
		bytes[0] = (byte) (b & 0xff);
		mem = mem + new String(bytes);

		if (mem.endsWith ("\n")) {
			mem = mem.substring (0, mem.length () - 1);
			flush ();
		}
	}

	/**
	 * Flushes the output stream.
	 */
	public void flush () {
		logger.info(mem);
		mem = "";
	}
}
