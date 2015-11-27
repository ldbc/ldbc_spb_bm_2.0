package eu.ldbc.semanticpublishing.tools;

import java.util.List;

import org.jfree.data.xy.XYSeries;

public class ChartToolBase {
	
	protected <T extends Number> XYSeries createSeries(List<T> dataset, String seriesTitle, int sampleIntervalSeconds) {
		XYSeries series = null;
		int iteration = 0;
		int samplesCounter = 0;
		
		if (dataset.size() > 0) {			 
			series = new XYSeries(seriesTitle);
				
			for (T t : dataset) {
				if (iteration % sampleIntervalSeconds == 0) {					
					series.add(t, samplesCounter++);
				}
				iteration++;
			}
		}
		
		return series;
	}	
}
