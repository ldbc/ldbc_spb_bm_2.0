package eu.ldbc.semanticpublishing.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

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
	private String spbResultFilePath;
	
	private static final String CHART_TITLE = "LDBC SPB Performance";
	
	private static final String SECONDS_STRING = "Seconds :";
	private static final String CURRENT_READS_STRING = "Current Queries";
	private static final String CURRENT_WRITES_STRING = "Current Operations";
	private static final String AVERAGE_READS_STRING = "Average Queries";
	private static final String AVERAGE_WRITES_STRING = "Average Operations";
	private static final String EDITORIAL_AGENTS = "Editorial:";
	private static final String AGGREGATION_AGENTS = "Aggregation:";
	
	private static final String PARAMETER_EXPORT_PNG = "exportPNG";
	
	public PerformanceChartTool(String spbResultsFilePath) {
		this.spbResultFilePath = spbResultsFilePath;
	}
	
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
       		CHART_TITLE + String.format(" [%s]", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())) + "\n(" + aggregationAgentsCount + " reading, " + editorialAgentsCount + " writing threads, duration: " + runTimeString + ")",			      													// Chart Title
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
        
        /*
XYDataset dataset1 = createXYVoltageDataset();
XYDataset dataset2 = createXYCurrentDataset();

XYLineAndShapeRenderer r1 = new XYLineAndShapeRenderer();
r1.setSeriesPaint(0, new Color(0xff, 0xff, 0x00)); 
r1.setSeriesPaint(1, new Color(0x00, 0xff, 0xff)); 
r1.setSeriesShapesVisible(0,  false);
r1.setSeriesShapesVisible(1,  false);

XYLineAndShapeRenderer r2 = new XYLineAndShapeRenderer();
r2.setSeriesPaint(0, new Color(0xff, 0x00, 0x00)); 
r2.setSeriesPaint(1, new Color(0x00, 0xff, 0x00)); 
r2.setSeriesShapesVisible(0,  false);
r2.setSeriesShapesVisible(1,  false);

JFreeChart chart = ChartFactory.createXYLineChart("Profile", "Set Current", "Voltage", null);
XYPlot plot = (XYPlot) chart.getPlot(); 

plot.setDataset(0, dataset1);
plot.setRenderer(0, r1);

plot.setDataset(1, dataset2);
plot.setRenderer(1, r2);

plot.setRangeAxis(1, new NumberAxis("Actual Current")); 
plot.mapDatasetToRangeAxis(1, 1); //2nd dataset to 2nd y-axi

plot.setBackgroundPaint(new Color(0xFF, 0xFF, 0xFF));
plot.setDomainGridlinePaint(new Color(0x00, 0x00, 0xff));
plot.setRangeGridlinePaint(new Color(0xff, 0x00, 0x00));         
         */
        
        return chart;
	}	
	
	public void exportAsPNG(String outputFile, JFreeChart chart, int width, int height ) throws FileNotFoundException 
	{
		File f = new File(outputFile);
		OutputStream out = new FileOutputStream(f);
		try { 
			BufferedImage chartImage = chart.createBufferedImage(width, height, null); 
			ImageIO.write(chartImage, "png", out); 
			out.close();
			System.out.println("Chart exported as .png at: " + f.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		} 
	} 
	
	public void showChart(int totalRunPeriodSeconds, int sampleIntervalSeconds, boolean exportAsPng) throws FileNotFoundException {
		long time = System.currentTimeMillis();		
		
		System.out.println("Rendering chart...");
		final XYDataset dataset = createChartDataset(currentReads, currentWrites, averageReads, averageWrites, sampleIntervalSeconds);
        final JFreeChart chart = createChart(dataset, totalRunPeriodSeconds, sampleIntervalSeconds);
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1024, 768));
        
        if (exportAsPng) {
        	exportAsPNG(spbResultFilePath + ".png", chart, 1024, 768);
        } else { 
	        final ApplicationFrame applicationFrame = new ApplicationFrame(CHART_TITLE);        
	        applicationFrame.setContentPane(chartPanel);
	        applicationFrame.setAlwaysOnTop(true);
	        applicationFrame.pack();
	        applicationFrame.setVisible(true);	        
        }
        System.out.println("Done in: " + (System.currentTimeMillis() - time) + " ms.");
	}
	
	public static void showHelp() {
		System.out.println("\n\tUsage: java -jar semantic_publishing_benchmark_chart_tool.jar [-exportPNG] <path_to_spb_results.log> <sampleIntervalSeconds>");
		System.out.println("\t\t<path_to_spb_results.log> \t- full path to SPB's benchmark result file");
		System.out.println("\t\t<sampleIntervalSeconds> \t- extract samples of results for each 'sampleIntervalSeconds' second. Allowed values: [1, MAX_INT]");
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			showHelp();
		}
		
		String fileName = args[0];
		int sampleIntervalSeconds = Integer.parseInt(args[1]);
		
		boolean exportAsPng = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].contains(PARAMETER_EXPORT_PNG)) {
				exportAsPng = true;
			}
		}
						
		System.out.println("Source result file 	: " + fileName);
		System.out.println("Samples interval   	: " + sampleIntervalSeconds + " s");
		System.out.println("Export to PNG		: " + exportAsPng);
		
		if (sampleIntervalSeconds < 1) {
			System.out.println("ERROR: Allowed samples Interval is: [1, MAX_INT]");
			return;
		}
		
		PerformanceChartTool pct = new PerformanceChartTool(fileName);
		int scannedRunPeriodSeconds = pct.initializeValues(fileName);
		pct.showChart(scannedRunPeriodSeconds, sampleIntervalSeconds, exportAsPng);
	}
}
