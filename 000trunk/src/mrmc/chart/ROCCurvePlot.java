/**
 * ROCCurvePlot.java
 * 
 * This software and documentation (the "Software") were developed at the Food and Drug Administration (FDA) 
 * by employees of the Federal Government in the course of their official duties. Pursuant to Title 17, Section 
 * 105 of the United States Code, this work is not subject to copyright protection and is in the public domain. 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of the Software, to deal in the 
 * Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, or sell copies of the Software or derivatives, and to permit persons to whom the 
 * Software is furnished to do so. FDA assumes no responsibility whatsoever for use by other parties of the 
 * Software, its source code, documentation or compiled executables, and makes no guarantees, expressed or 
 * implied, about its quality, reliability, or any other characteristic.   Further, use of this code in no way 
 * implies endorsement by the FDA or confers any advantage in regulatory decisions.  Although this software 
 * can be redistributed and/or modified freely, we ask that any derivative works bear some notice that they 
 * are derived from it, and any modified versions bear some notice that they have been modified.
 */

package mrmc.chart;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Creates a chart displaying ROC curves for each reader of a particular
 * modality. Reader curves are then averaged in direction of sensitivity
 * (vertically), specificity (horizontally), and sensitivity + specificity
 * (diagonally). Additionally all reader scores are placed in one set and a
 * curve is determined, creating a "pooled average"
 * 
 * @author Rohan Pathare
 */
public class ROCCurvePlot extends JFrame {

	private static final long serialVersionUID = 1L;
	private XYLineAndShapeRenderer renderer;
	private XYSeriesCollection seriesCollection;
	private ArrayList<InterpolatedLine> allLines;
	private ArrayList<String> readerSeriesTitles;
	private ArrayList<JCheckBox> readerSeriesBoxes;
	private JCheckBox vert;
	private JCheckBox horiz;
	private JCheckBox diag;
	private JCheckBox pooled;

	/**
	 * Sole constructor. Creates a line plot display ROC curves
	 * 
	 * @param title Title of the chart
	 * @param xaxis x-axis label
	 * @param yaxis y-axis label
	 * @param treeMap Mapping of readers to a set of points defining an ROC curve
	 */
	public ROCCurvePlot(final String title, String xaxis, String yaxis,
			TreeMap<String, TreeSet<XYPair>> treeMap) {
		super(title);

		createDataset(treeMap);
		final JFreeChart chart = ChartFactory.createScatterPlot(title, xaxis,
				yaxis, seriesCollection, PlotOrientation.VERTICAL, true, true,
				false);
		XYPlot xyplot = (XYPlot) chart.getPlot();
		xyplot.setDomainCrosshairVisible(true);
		xyplot.setRangeCrosshairVisible(true);
		NumberAxis domain = (NumberAxis) xyplot.getDomainAxis();
		domain.setRange(0.00, 1.00);
		domain.setTickUnit(new NumberTickUnit(0.1));
		NumberAxis range = (NumberAxis) xyplot.getRangeAxis();
		range.setRange(0.00, 1.00);
		range.setTickUnit(new NumberTickUnit(0.1));
		renderer = new XYLineAndShapeRenderer();
		chart.getXYPlot().setRenderer(renderer);
		ChartPanel chartPanel = new ChartPanel(chart);

		JPanel readerSelect = new JPanel(new WrapLayout());
		readerSeriesBoxes = new ArrayList<JCheckBox>();

		for (String r : treeMap.keySet()) {
			JCheckBox aBox = new JCheckBox("" + r);
			aBox.setSelected(false);
			aBox.addItemListener(new SeriesSelectListener());
			hideSeries("" + r);
			readerSeriesBoxes.add(aBox);
			readerSelect.add(aBox);
		}

		renderer.setSeriesShapesVisible(
				seriesCollection.getSeriesIndex("Vertical Average"), false);
		renderer.setSeriesStroke(
				seriesCollection.getSeriesIndex("Vertical Average"),
				new java.awt.BasicStroke(3f));
		renderer.setSeriesShapesVisible(
				seriesCollection.getSeriesIndex("Horizontal Average"), false);
		renderer.setSeriesStroke(
				seriesCollection.getSeriesIndex("Horizontal Average"),
				new java.awt.BasicStroke(3f));
		renderer.setSeriesShapesVisible(
				seriesCollection.getSeriesIndex("Diagonal Average"), false);
		renderer.setSeriesStroke(
				seriesCollection.getSeriesIndex("Diagonal Average"),
				new java.awt.BasicStroke(3f));
		renderer.setSeriesStroke(
				seriesCollection.getSeriesIndex("Pooled Average"),
				new java.awt.BasicStroke(3f));

		vert = new JCheckBox("Vertical Average");
		vert.setSelected(true);
		vert.addItemListener(new SeriesSelectListener());
		readerSelect.add(vert);
		horiz = new JCheckBox("Horizontal Average");
		horiz.setSelected(true);
		horiz.addItemListener(new SeriesSelectListener());
		readerSelect.add(horiz);
		diag = new JCheckBox("Diagonal Average");
		diag.setSelected(true);
		diag.addItemListener(new SeriesSelectListener());
		readerSelect.add(diag);
		pooled = new JCheckBox("Pooled Average");
		pooled.setSelected(true);
		pooled.addItemListener(new SeriesSelectListener());
		readerSelect.add(pooled);

		JCheckBox allReaders = new JCheckBox("Show Readers");
		allReaders.setSelected(false);
		allReaders.addItemListener(new ReadersSelectListener());
		readerSelect.add(allReaders);
		JCheckBox allAverages = new JCheckBox("Show Averages");
		allAverages.setSelected(true);
		allAverages.addItemListener(new AverageSelectListener());
		readerSelect.add(allAverages);

		chartPanel.setPreferredSize(new java.awt.Dimension(700, 700));
		this.add(chartPanel);
		this.add(readerSelect, BorderLayout.PAGE_END);

	}

	/**
	 * Converts the mapping of readers to curve points into a collection of
	 * separate XY data.
	 * 
	 * @param treeMap Mapping of readers to points defining a curve
	 */
	private void createDataset(TreeMap<String, TreeSet<XYPair>> treeMap) {
		seriesCollection = new XYSeriesCollection();
		readerSeriesTitles = new ArrayList<String>();

		for (String r : treeMap.keySet()) {
			XYSeries series = new XYSeries("" + r, false);
			readerSeriesTitles.add("" + r);
			for (XYPair point : treeMap.get(r)) {
				series.add(point.x, point.y);
			}
			seriesCollection.addSeries(series);
		}

		allLines = new ArrayList<InterpolatedLine>();
		for (String r : treeMap.keySet()) {
			allLines.add(new InterpolatedLine(treeMap.get(r)));
		}

		XYSeries vertAvg = generateVerticalROC();
		seriesCollection.addSeries(vertAvg);
		XYSeries horizAvg = generateHorizontalROC();
		seriesCollection.addSeries(horizAvg);
		XYSeries diagAvg = generateDiagonalROC(treeMap);
		seriesCollection.addSeries(diagAvg);
		XYSeries pooledAvg = new XYSeries("Pooled Average", false);
		seriesCollection.addSeries(pooledAvg);

	}

	/**
	 * Adds a set of XY points to the collection of ROC curves
	 * 
	 * @param newData Set of XY coordinates
	 * @param type Name for this set of points
	 */
	public void addData(TreeSet<XYPair> newData, String type) {
		for (XYPair point : newData) {
			seriesCollection.getSeries(type).add(point.x, point.y);
		}
	}

	/**
	 * Creates an ROC curve that averages together the scores for all readers in
	 * the diagonal direction
	 * 
	 * @param treeMap Mapping of readers to points defining a curve
	 * @return Series containing the ROC curve points
	 */
	private XYSeries generateDiagonalROC(TreeMap<String, TreeSet<XYPair>> treeMap) {
		XYSeries diagAvg = new XYSeries("Diagonal Average", false);
		TreeMap<String, TreeSet<XYPair>> rotatedData = new TreeMap<String, TreeSet<XYPair>>();

		// rotate all points in data 45 degrees clockwise about origin
		for (String r : treeMap.keySet()) {
			rotatedData.put(r, new TreeSet<XYPair>());
			for (XYPair point : treeMap.get(r)) {
				double x2 = (point.x + point.y) / Math.sqrt(2.0);
				double y2 = (point.y - point.x) / Math.sqrt(2.0);
				rotatedData.get(r).add(new XYPair(x2, y2));
			}
		}

		// generate linear interpolation with new points
		ArrayList<InterpolatedLine> rotatedLines = new ArrayList<InterpolatedLine>();
		for (String r : rotatedData.keySet()) {
			rotatedLines.add(new InterpolatedLine(rotatedData.get(r)));
		}

		// take vertical sample averages from x = 0 to x = 1
		for (double i = 0; i <= Math.sqrt(2); i += 0.01) {
			double avg = 0;
			int counter = 0;
			for (InterpolatedLine line : rotatedLines) {
				avg += line.getYatDiag(i);
				counter++;
			}

			// rotate points back 45 degrees counterclockwise
			double x1 = i;
			double y1 = (avg / (double) counter);
			double x2 = (x1 * Math.cos(Math.toRadians(45)))
					- (y1 * Math.sin(Math.toRadians(45)));
			double y2 = (x1 * Math.sin(Math.toRadians(45)))
					+ (y1 * Math.cos(Math.toRadians(45)));
			diagAvg.add(x2, y2);
		}

		diagAvg.add(1, 1);
		return diagAvg;
	}

	/**
	 * Creates an ROC curve that averages together the scores for all readers in
	 * the horizontal direction
	 * 
	 * @return Series containing the ROC curve points
	 */
	private XYSeries generateHorizontalROC() {
		XYSeries horizAvg = new XYSeries("Horizontal Average", false);
		for (double i = 0; i <= 1.01; i += 0.01) {
			double avg = 0;
			int counter = 0;
			for (InterpolatedLine line : allLines) {
				avg += line.getXat(i);
				counter++;
			}
			horizAvg.add(avg / (double) counter, i);
		}
		return horizAvg;
	}

	/**
	 * Creates an ROC curve that averages together the scores for all readers in
	 * the vertical direction
	 * 
	 * @return Series containing the ROC curve points
	 */
	private XYSeries generateVerticalROC() {
		XYSeries vertAvg = new XYSeries("Vertical Average", false);
		for (double i = 0; i <= 1.01; i += 0.01) {
			double avg = 0;
			int counter = 0;
			for (InterpolatedLine line : allLines) {
				avg += line.getYat(i);
				counter++;
			}
			vertAvg.add(i, avg / (double) counter);
		}
		return vertAvg;
	}

	/**
	 * Hides the specified series from the chart
	 * 
	 * @param series Which series to hide
	 */
	private void hideSeries(String series) {
		renderer.setSeriesLinesVisible(
				(seriesCollection.getSeriesIndex(series)), false);
		renderer.setSeriesShapesVisible(
				(seriesCollection.getSeriesIndex(series)), false);
		renderer.setSeriesVisibleInLegend(
				(seriesCollection.getSeriesIndex(series)), false);
	}

	/**
	 * Displays the specified series on the chart
	 * 
	 * @param series Which series to display
	 * @param shapes Whether or not to display shapes indicating individual data
	 *            points
	 */
	private void showSeries(String series, boolean shapes) {
		renderer.setSeriesLinesVisible(
				(seriesCollection.getSeriesIndex(series)), true);
		if (shapes) {
			renderer.setSeriesShapesVisible(
					(seriesCollection.getSeriesIndex(series)), true);
		}
		renderer.setSeriesVisibleInLegend(
				(seriesCollection.getSeriesIndex(series)), true);
	}

	/**
	 * Handler for checkbox to show or hide all readers from chart
	 */
	class ReadersSelectListener implements ItemListener {
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				for (JCheckBox readerBox : readerSeriesBoxes) {
					readerBox.setSelected(false);
				}
				for (String title : readerSeriesTitles) {
					hideSeries(title);
				}
			} else if (e.getStateChange() == ItemEvent.SELECTED) {
				for (JCheckBox readerBox : readerSeriesBoxes) {
					readerBox.setSelected(true);
				}
				for (String title : readerSeriesTitles) {
					showSeries(title, true);
				}
			}
		}
	}

	/**
	 * Handler for checkbox to show or hide all averages from chart
	 */
	class AverageSelectListener implements ItemListener {
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				vert.setSelected(false);
				horiz.setSelected(false);
				diag.setSelected(false);
				pooled.setSelected(false);
				hideSeries("Vertical Average");
				hideSeries("Horizontal Average");
				hideSeries("Diagonal Average");
				hideSeries("Pooled Average");
			} else if (e.getStateChange() == ItemEvent.SELECTED) {
				vert.setSelected(true);
				horiz.setSelected(true);
				diag.setSelected(true);
				pooled.setSelected(true);
				showSeries("Vertical Average", false);
				showSeries("Horizontal Average", false);
				showSeries("Diagonal Average", false);
				showSeries("Pooled Average", true);
			}

		}
	}

	/**
	 * Handler for checkboxes to show/hide a specific series
	 */
	class SeriesSelectListener implements ItemListener {
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				if (((JCheckBox) e.getItem()).getText().equals(
						"Vertical Average")
						|| ((JCheckBox) e.getItem()).getText().equals(
								"Horizontal Average")
						|| ((JCheckBox) e.getItem()).getText().equals(
								"Diagonal Average")) {
					hideSeries(((JCheckBox) e.getItem()).getText());
				} else {
					hideSeries(((JCheckBox) e.getItem()).getText());
				}
			} else if (e.getStateChange() == ItemEvent.SELECTED) {
				if (((JCheckBox) e.getItem()).getText().equals(
						"Vertical Average")
						|| ((JCheckBox) e.getItem()).getText().equals(
								"Horizontal Average")
						|| ((JCheckBox) e.getItem()).getText().equals(
								"Diagonal Average")) {
					showSeries(((JCheckBox) e.getItem()).getText(), false);
				} else {
					showSeries(((JCheckBox) e.getItem()).getText(), true);
				}
			}
		}
	}
}
