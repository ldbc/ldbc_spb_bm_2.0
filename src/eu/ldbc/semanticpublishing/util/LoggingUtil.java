package eu.ldbc.semanticpublishing.util;

import java.io.File;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import eu.ldbc.semanticpublishing.properties.Configuration;

/**
 * A class used for configuring the log4j logging system.
 * Currently 3 separate log files will be generated :
 *   - semantic_publishing_benchmark_queries_brief.log - contains the execution order of all queries during the benchmark run.                                                     
 *   - semantic_publishing_benchmark_queries_detailed.log - contains all SPARQL queries and their result in RDF-XML.
 *   - semantic_publishing_benchmark_results.log - contains the results of the benchmark, generated each second.                                                                 
 */
public class LoggingUtil {
	private static final String EXECUTED_QUERIES_BRIEF_LOG_FILE_NAME = "semantic_publishing_benchmark_queries_brief.log";
	private static final String EXECUTED_QUERIES_BRIEF_FILE_APPENDER_NAME = "executed.queries.brief.file.appender";
	private static final String EXECUTED_QUERIES_DETAILED_LOG_FILE_NAME = "semantic_publishing_benchmark_queries_detailed.log";
	private static final String EXECUTED_QUERIES_DETAILED_FILE_APPENDER_NAME = "executed.queries.detailed.file.appender";
	private static final String BENCHMARK_RESULTS_LOG_FILE_NAME = "semantic_publishing_benchmark_results.log";
	private static final String BENCHMARK_RESULTS_FILE_APPENDER_NAME = "benchmark.results.file.appender";
	private static final String SESAME_CONSOLE_APPPENDER_NAME = "sesame.console.appender";
	
	private static final String MAX_LOG_FILE_SIZE = "250MB";
	private static final int MAX_LOG_FILE_BACKUP_INDEX = 50000;
	
	private static final String LAYOUT = ">> %d{HH:mm:ss.SSS} [%c{1}:%t] : %m\n";
	private static final String LAYOUT_SIMPLE = "%d{HH:mm:ss.SSS} : %m\n";
	
	private static final String LOGS_FOLDER = "logs";
	
	public static void Configure(Configuration configuration) {
		if (!configuration.getBoolean(Configuration.ENABLE_LOGS)) {
			System.out.println("Logging has been disabled...");
			return;
		}
			
		//Appender for brief query execution log
		RollingFileAppender briefQueriesLogFileAppender = new RollingFileAppender();
		briefQueriesLogFileAppender.setName(EXECUTED_QUERIES_BRIEF_FILE_APPENDER_NAME);
		briefQueriesLogFileAppender.setFile(LOGS_FOLDER + File.separator + EXECUTED_QUERIES_BRIEF_LOG_FILE_NAME);
		briefQueriesLogFileAppender.setLayout(new PatternLayout(LAYOUT_SIMPLE));
		briefQueriesLogFileAppender.setThreshold(Level.INFO);
		briefQueriesLogFileAppender.setAppend(false);
		briefQueriesLogFileAppender.setBufferedIO(false);
		briefQueriesLogFileAppender.setMaxFileSize(MAX_LOG_FILE_SIZE);
		briefQueriesLogFileAppender.setMaxBackupIndex(MAX_LOG_FILE_BACKUP_INDEX);
		briefQueriesLogFileAppender.activateOptions();
		
		Logger.getLogger("eu.ldbc.semanticpublishing.TestDriver").addAppender(briefQueriesLogFileAppender);
		
		//Appender for detailed query execution log, queries and results
		RollingFileAppender detailedQueriesLogFileAppender = new RollingFileAppender();
		detailedQueriesLogFileAppender.setName(EXECUTED_QUERIES_DETAILED_FILE_APPENDER_NAME);
		detailedQueriesLogFileAppender.setFile(LOGS_FOLDER + File.separator + EXECUTED_QUERIES_DETAILED_LOG_FILE_NAME);
		detailedQueriesLogFileAppender.setLayout(new PatternLayout(LAYOUT));
		detailedQueriesLogFileAppender.setThreshold(Level.INFO);
		detailedQueriesLogFileAppender.setAppend(false);
		detailedQueriesLogFileAppender.setBufferedIO(false);
		detailedQueriesLogFileAppender.setMaxFileSize(MAX_LOG_FILE_SIZE);
		detailedQueriesLogFileAppender.setMaxBackupIndex(MAX_LOG_FILE_BACKUP_INDEX);
		detailedQueriesLogFileAppender.activateOptions();

		Logger.getLogger("eu.ldbc.semanticpublishing.agents.AggregationAgent").addAppender(detailedQueriesLogFileAppender);
		Logger.getLogger("eu.ldbc.semanticpublishing.agents.EditorialAgent").addAppender(detailedQueriesLogFileAppender);
		
		//Appender for benchmark results
		RollingFileAppender benchmarkResultsFileAppander = new RollingFileAppender();
		benchmarkResultsFileAppander.setName(BENCHMARK_RESULTS_FILE_APPENDER_NAME);
		benchmarkResultsFileAppander.setFile(LOGS_FOLDER + File.separator + BENCHMARK_RESULTS_LOG_FILE_NAME);
		benchmarkResultsFileAppander.setLayout(new PatternLayout(LAYOUT_SIMPLE));
		benchmarkResultsFileAppander.setThreshold(Level.INFO);
		benchmarkResultsFileAppander.setAppend(false);
		benchmarkResultsFileAppander.setBufferedIO(false);
		benchmarkResultsFileAppander.setMaxFileSize(MAX_LOG_FILE_SIZE);
		benchmarkResultsFileAppander.setMaxBackupIndex(MAX_LOG_FILE_BACKUP_INDEX);
		benchmarkResultsFileAppander.activateOptions();
		
		Logger.getLogger("eu.ldbc.semanticpublishing.BenchmarkProcessObserver").addAppender(benchmarkResultsFileAppander);
		
		//Console Appender for Sesame
		ConsoleAppender sesameConsoleAppender = new ConsoleAppender();
		sesameConsoleAppender.setName(SESAME_CONSOLE_APPPENDER_NAME);
		sesameConsoleAppender.setLayout(new PatternLayout(LAYOUT_SIMPLE));
		sesameConsoleAppender.setThreshold(Level.INFO);
		
		Logger.getLogger("org.openrdf").addAppender(sesameConsoleAppender);
	}
}
