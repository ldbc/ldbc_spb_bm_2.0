package eu.ldbc.semanticpublishing.tools;

import java.util.List;

import org.jfree.data.xy.XYSeries;

public class ChartToolBase {
	protected XYSeries createSeries(List<Long> dataset, String seriesTitle, int sampleIntervalSeconds) {
		XYSeries series = null;
		int iteration = 0;
		int samplesCounter = 0;
		
		if (dataset.size() > 0) {			 
			series = new XYSeries(seriesTitle);
		
			for (Long l : dataset) {
				if (iteration % sampleIntervalSeconds == 0) {
					series.add(l.longValue(), samplesCounter++);
				}
				iteration++;
			}
		}
		
		return series;
	}
}
