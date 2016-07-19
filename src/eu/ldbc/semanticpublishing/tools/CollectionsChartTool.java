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

public class CollectionsChartTool extends ChartToolBase {
	
	private static final String CHART_TITLE = "Collection Stats";
	
	private static final String SPLIT_DELIMITER = " \\| ";
	private static final String SPLIT_DELIMITER_INNER = ":";
	
	private static final String CACHE_HITS_STRING = "Cache Hits";
	private static final String CACHE_MISSES_STRING = "Cache Misses";
	private static final String PAGE_DISCARDS_STRING = "Page Discards";
	private static final String PAGE_SWAPS_STRING = "Page Swaps";
	private static final String READS_STRING = "Reads";
	private static final String WRITES_STRING = "Writes";
	
	private List<Long> cacheHits = new LinkedList<Long>();
	private List<Long> cacheMisses = new LinkedList<Long>();
	private List<Long> pageDiscards = new LinkedList<Long>();
	private List<Long> pageSwaps = new LinkedList<Long>();
	private List<Long> reads = new LinkedList<Long>();
	private List<Long> writes = new LinkedList<Long>();
	
	/**
	 * @param spbResultFilePath - file with stats from JMX collections
	 * @param collectionType - recommended options ("POS", "PSO", "PCOS", "PCSO")
	 * @return
	 * @throws IOException
	 */
	private int initializeValues(String spbResultFilePath, String collectionType) throws IOException {
		long time = System.currentTimeMillis();
		System.out.println("Initializing values from result file: " + spbResultFilePath);
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(spbResultFilePath), "UTF-8"));
		
		long value = 0;
		int samplesCount = 0;
		int linesCount = 0;
		String line;
		
		while ((line = in.readLine()) != null) {
			linesCount++;
			if (linesCount % 1000000 == 0) {
				System.out.println(String.format("\tparsed: %,11d lines...", linesCount));
			}
			if (!line.trim().startsWith(collectionType)) {
				 continue;
			}
			
			String[] tokens = line.split(SPLIT_DELIMITER);
			
			if (tokens.length != 7) {
				System.out.println("WARNING: unexpected number of tokens after the split: " + tokens.length);
				continue;
			}
			
			samplesCount++;
			
			//CacheHits
			value = Long.parseLong(tokens[1].split(SPLIT_DELIMITER_INNER)[1].trim());
			if (cacheHits.size() > 0) {
				//compute the delta				
				value -= cacheHits.get(cacheHits.size() - 1);
			}
			cacheHits.add(value);
			
			//CacheMisses
			value = Long.parseLong(tokens[2].split(SPLIT_DELIMITER_INNER)[1].trim());
			if (cacheMisses.size() > 0) {
				//compute the delta				
				value -= cacheMisses.get(cacheMisses.size() - 1);
			}
			cacheMisses.add(value);			
			
			//PageDiscards
			value = Long.parseLong(tokens[3].split(SPLIT_DELIMITER_INNER)[1].trim());
			if (pageDiscards.size() > 0) {
				//compute the delta				
				value -= pageDiscards.get(pageDiscards.size() - 1);
			}
			pageDiscards.add(value);	
			
			//pageSwaps
			value = Long.parseLong(tokens[4].split(SPLIT_DELIMITER_INNER)[1].trim());
			if (pageSwaps.size() > 0) {
				//compute the delta				
				value -= pageSwaps.get(pageSwaps.size() - 1);
			}
			pageSwaps.add(value);
			
			//reads
			value = Long.parseLong(tokens[5].split(SPLIT_DELIMITER_INNER)[1].trim());
			if (reads.size() > 0) {
				//compute the delta				
				value -= reads.get(reads.size() - 1);
			}
			reads.add(value);
			
			//writes
			value = Long.parseLong(tokens[6].split(SPLIT_DELIMITER_INNER)[1].trim());
			if (writes.size() > 0) {
				//compute the delta				
				value -= writes.get(writes.size() - 1);
			}
			writes.add(value);			
		}			
		in.close();
		System.out.println("Finished in: " + (System.currentTimeMillis() - time) + " ms.");
		
		return samplesCount;
	}
	
	private XYDataset createChartDataset(List<Long> cacheHits, List<Long> cacheMisses, List<Long> pageDiscards, List<Long> pageSwaps, List<Long> reads, List<Long> writes, int sampleIntervalSeconds) {
		final XYSeries cacheHitsSeries;
		final XYSeries cacheMissesSeries;
		final XYSeries pageDiscardsSeries;
		final XYSeries pageSwapsSeries;
		final XYSeries readsSeries;
		final XYSeries writesSeries;
		 
		final XYSeriesCollection chartDataset = new XYSeriesCollection();
		
		cacheHitsSeries = createSeries(cacheHits, CACHE_HITS_STRING, sampleIntervalSeconds);
		if (cacheHitsSeries != null) {
			chartDataset.addSeries(cacheHitsSeries);
		}
		
		cacheMissesSeries = createSeries(cacheMisses, CACHE_MISSES_STRING, sampleIntervalSeconds);
		if (cacheMissesSeries != null) {
			chartDataset.addSeries(cacheMissesSeries);
		}
		
		pageDiscardsSeries = createSeries(pageDiscards, PAGE_DISCARDS_STRING, sampleIntervalSeconds);
		if (pageDiscardsSeries != null) {
			chartDataset.addSeries(pageDiscardsSeries);
		}
		
		pageSwapsSeries = createSeries(pageSwaps, PAGE_SWAPS_STRING, sampleIntervalSeconds);
		if (pageSwapsSeries != null) {
			chartDataset.addSeries(pageSwapsSeries);
		}
		
		readsSeries = createSeries(reads, READS_STRING, sampleIntervalSeconds);
		if (readsSeries != null) {
			chartDataset.addSeries(readsSeries);
		}
		
		writesSeries = createSeries(writes, WRITES_STRING, sampleIntervalSeconds);
		if (writesSeries != null) {
			chartDataset.addSeries(writesSeries);
		}
		
		return chartDataset;
	}
	
	private JFreeChart createChart(final XYDataset dataset, int totalRunTimeSeconds , int timeSampleIntervalSeconds, String collectionType) {
        
//		int hours = totalRunTimeSeconds / (60 * 60);
//		int minutes = totalRunTimeSeconds % (60 * 60) / 60;
//		int seconds = totalRunTimeSeconds % 60;
		
//		String runTimeString = String.format("%d hours %d minutes %s seconds", hours, minutes, seconds);
		
        final JFreeChart chart = ChartFactory.createXYLineChart(
       		CHART_TITLE + " (" + collectionType.toUpperCase() + ")",	      													// Chart Title
            "Operations/s",    																									// Y axis label
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
	
	public void showChart(int totalRunPeriodSeconds, int sampleIntervalSeconds, String collectionType) {
		long time = System.currentTimeMillis();
		System.out.println("Rendering chart...");
		
		final XYDataset dataset = createChartDataset(cacheHits, cacheMisses, pageDiscards, pageSwaps, reads, writes, sampleIntervalSeconds);
        final JFreeChart chart = createChart(dataset, totalRunPeriodSeconds, sampleIntervalSeconds, collectionType);
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
		System.out.println("TODO... see java code...");
//		System.out.println("\n\tUsage: java -jar semantic_publishing_benchmark_chart_tool.jar <path_to_spb_results.log> <sampleIntervalSeconds>");
//		System.out.println("\t\t<path_to_spb_results.log> \t- full path to SPB's benchmark result file");
//		System.out.println("\t\t<sampleIntervalSeconds> \t- extract samples of results for each 'sampleIntervalSeconds' second. Allowed values: [1, MAX_INT]");
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			showHelp();
		}
		
		String fileName = args[0];
		int sampleIntervalSeconds = Integer.parseInt(args[1]);
		String collectionType = args[2];
		
		System.out.println("Source result file : " + fileName);
		System.out.println("Samples interval   : " + sampleIntervalSeconds + " s");
		System.out.println("Collection type    : " + collectionType);
		
		if (sampleIntervalSeconds < 1) {
			System.out.println("ERROR: Allowed samples Interval is: [1, MAX_INT]");
			return;
		}
		
		CollectionsChartTool cct = new CollectionsChartTool();
		int scannedRunPeriodSeconds = cct.initializeValues(fileName, collectionType);
		cct.showChart(scannedRunPeriodSeconds, sampleIntervalSeconds, collectionType);	
	}
}
