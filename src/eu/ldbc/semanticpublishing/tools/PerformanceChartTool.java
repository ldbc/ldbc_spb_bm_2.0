package eu.ldbc.semanticpublishing.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

public class PerformanceChartTool extends ChartToolBase {
	private List<Double> currentReads = new LinkedList<Double>();
	private List<Double> currentWrites = new LinkedList<Double>();
	private List<Double> averageReads = new LinkedList<Double>();
	private List<Double> averageWrites = new LinkedList<Double>();
	private int editorialAgentsCount = -1;
	private int aggregationAgentsCount = -1;
	
	private static final String CHART_TITLE = "LDBC SPB Performance";
	
	private static final String SECONDS_STRING = "Seconds :";
	private static final String CURRENT_READS_STRING = "Current Queries";
	private static final String CURRENT_WRITES_STRING = "Current Operations";
	private static final String AVERAGE_READS_STRING = "Average Queries";
	private static final String AVERAGE_WRITES_STRING = "Average Operations";
	private static final String EDITORIAL_AGENTS = "Editorial:";
	private static final String AGGREGATION_AGENTS = "Aggregation:";
	
	private int initializeValues(String spbResultFilePath) throws IOException {
		long time = System.currentTimeMillis();
		System.out.println("Initializing values from result file: " + spbResultFilePath);
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(spbResultFilePath), "UTF-8"));
		
		int reportsScanned = 0;
		double value = 0f;
		int linesCount = 0;
		String line;
		int editorialAgentsNumnberInitialized = 0;
		int aggregationAgentsNumberInitialized = 0;
		
		while ((line = in.readLine()) != null) {
			linesCount++;
			if (linesCount % 1000000 == 0) {
				System.out.println(String.format("\tparsed: %,11d lines...", linesCount));
			}
			if (line.contains(SECONDS_STRING)) {
				reportsScanned++;
			}
			
			if (editorialAgentsNumnberInitialized < 2) {
				if (editorialAgentsNumnberInitialized == 1) {
					editorialAgentsCount = Integer.parseInt(line.trim().substring(0, line.trim().indexOf(" "))); 
					editorialAgentsNumnberInitialized++;
				}
				
				if (line.contains(EDITORIAL_AGENTS)) {
					editorialAgentsNumnberInitialized++;
					continue;
				}
			}
			
			if (aggregationAgentsNumberInitialized < 2) {
				if (aggregationAgentsNumberInitialized == 1) {
					aggregationAgentsCount = Integer.parseInt(line.trim().substring(0, line.trim().indexOf(" ")));
					aggregationAgentsNumberInitialized++;
				}
				
				if (line.contains(AGGREGATION_AGENTS)) {
					aggregationAgentsNumberInitialized++;
					continue;
				}
			}
			
			if (line.contains(CURRENT_WRITES_STRING.toLowerCase())) {
				value = Double.parseDouble(line.trim().substring(0, line.trim().indexOf(CURRENT_WRITES_STRING.toLowerCase())).trim());
				currentWrites.add(value);
			} else if (line.contains(AVERAGE_WRITES_STRING.toLowerCase())) {
				value = Double.parseDouble(line.trim().substring(0, line.trim().indexOf(AVERAGE_WRITES_STRING.toLowerCase())).trim());
				averageWrites.add(value);
			} else if (line.contains(CURRENT_READS_STRING.toLowerCase())) {
				value = Double.parseDouble(line.trim().substring(0, line.trim().indexOf(CURRENT_READS_STRING.toLowerCase())).trim());
				currentReads.add(value);
			} else if (line.contains(AVERAGE_READS_STRING.toLowerCase())) {
				value = Double.parseDouble(line.trim().substring(0, line.trim().indexOf(AVERAGE_READS_STRING.toLowerCase())).trim());
				averageReads.add(value);
			} else {
				continue;
			}			
		}			
		in.close();
		System.out.println("Finished in: " + (System.currentTimeMillis() - time) + " ms.");
		
		return reportsScanned;
	}
	
	private XYDataset createChartDataset(List<Double> currentReads, List<Double> currentWrites, List<Double> averageReads, List<Double> averageWrites, int sampleIntervalSeconds) {
		final XYSeries currentReadsSeries;
		final XYSeries currentWritesSeries;
		final XYSeries averageReadsSeries;
		final XYSeries averageWritesSeries;
		 
		final XYSeriesCollection chartDataset = new XYSeriesCollection();
		
		if (currentReads.size() > 0) {	
			currentReadsSeries = createSeries(currentReads, CURRENT_READS_STRING, sampleIntervalSeconds);
			if (currentReadsSeries != null) {
				chartDataset.addSeries(currentReadsSeries);
			}			
		}
		 
		 
		if (currentWrites.size() > 0) {
			currentWritesSeries = createSeries(currentWrites, CURRENT_WRITES_STRING, sampleIntervalSeconds);
			if (currentWritesSeries != null) {
				chartDataset.addSeries(currentWritesSeries);
			}	
		}
		 
		if (averageReads.size() > 0) {
			averageReadsSeries = createSeries(averageReads, AVERAGE_READS_STRING, sampleIntervalSeconds);
			if (averageReadsSeries != null) {
				chartDataset.addSeries(averageReadsSeries);
			}	
		}
		
		if (averageWrites.size() > 0) {
			averageWritesSeries = createSeries(averageWrites, AVERAGE_WRITES_STRING, sampleIntervalSeconds);
			if (averageWritesSeries != null) {
				chartDataset.addSeries(averageWritesSeries);
			}	
		}
		
		return chartDataset;
	}
	
	private JFreeChart createChart(final XYDataset dataset, int totalRunTimeSeconds , int timeSampleIntervalSeconds) {
        
		int hours = totalRunTimeSeconds / (60 * 60);
		int minutes = totalRunTimeSeconds % (60 * 60) / 60;
		int seconds = totalRunTimeSeconds % 60;
		
		String runTimeString = String.format("%d hours %d minutes %s seconds", hours, minutes, seconds);
		
        final JFreeChart chart = ChartFactory.createXYLineChart(
       		CHART_TITLE + " (" + aggregationAgentsCount + " reading, " + editorialAgentsCount + " writing threads)" + "\n(duration: " + runTimeString + ")",			      													// Chart Title
            "Queries/s",      																									// Y axis label
            "Time (samples at: " + timeSampleIntervalSeconds + (timeSampleIntervalSeconds > 1 ? " seconds" : " second") + ")",	// X axis label
            dataset,       																										// Data
            PlotOrientation.HORIZONTAL,
            true,      																											// Include legend
            true,             																									// Tooltips
            false             																									// URLs
        );
        
        chart.setBackgroundPaint(Color.white);
        
//      final StandardLegend legend = (StandardLegend) chart.getLegend();
//      legend.setDisplaySeriesShapes(true);
      
        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
//      plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.gray);
        plot.setRangeGridlinePaint(Color.gray);
      
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
      
        for (int i = 0; i < plot.getSeriesCount(); i++) {
        	renderer.setSeriesLinesVisible(i, false);      
        	renderer.setSeriesShapesVisible(i, true);
        }
      
        plot.setRenderer(renderer);

        // change the auto tick unit selection to integer units only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());        
        
        return chart;
	}	
	
	public void showChart(int totalRunPeriodSeconds, int sampleIntervalSeconds) {
		long time = System.currentTimeMillis();
		System.out.println("Rendering chart...");
		
		final XYDataset dataset = createChartDataset(currentReads, currentWrites, averageReads, averageWrites, sampleIntervalSeconds);
        final JFreeChart chart = createChart(dataset, totalRunPeriodSeconds, sampleIntervalSeconds);
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1024, 768));
        
        final ApplicationFrame applicationFrame = new ApplicationFrame(CHART_TITLE);        
        applicationFrame.setContentPane(chartPanel);
        applicationFrame.setAlwaysOnTop(true);
        applicationFrame.pack();
        applicationFrame.setVisible(true);
        
        System.out.println("Done in: " + (System.currentTimeMillis() - time) + " ms.");
	}
	
	public static void showHelp() {
		System.out.println("\n\tUsage: java -jar semantic_publishing_benchmark_chart_tool.jar <path_to_spb_results.log> <sampleIntervalSeconds>");
		System.out.println("\t\t<path_to_spb_results.log> \t- full path to SPB's benchmark result file");
		System.out.println("\t\t<sampleIntervalSeconds> \t- extract samples of results for each 'sampleIntervalSeconds' second. Allowed values: [1, MAX_INT]");
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			showHelp();
		}
		
		String fileName = args[0];
		int sampleIntervalSeconds = Integer.parseInt(args[1]);
		
		System.out.println("Source result file : " + fileName);
		System.out.println("Samples interval   : " + sampleIntervalSeconds + " s");
		
		if (sampleIntervalSeconds < 1) {
			System.out.println("ERROR: Allowed samples Interval is: [1, MAX_INT]");
			return;
		}
		
		PerformanceChartTool pct = new PerformanceChartTool();
		int scannedRunPeriodSeconds = pct.initializeValues(fileName);
		pct.showChart(scannedRunPeriodSeconds, sampleIntervalSeconds);
	}
}
