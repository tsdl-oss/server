package org.tsdl.testutil.visualization.impl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsdl.infrastructure.model.DataPoint;
import org.tsdl.testutil.visualization.api.TimeSeriesTestVisualizer;
import org.tsdl.testutil.visualization.api.TsdlTestInfo;
import org.tsdl.testutil.visualization.api.TsdlTestVisualization;

/**
 * Implementation of {@link TimeSeriesTestVisualizer} depending on the JFreeChart visualization library.
 */
public class JfreeChartTimeSeriesTestVisualizer implements TimeSeriesTestVisualizer {
  private static final Logger LOGGER = LoggerFactory.getLogger(JfreeChartTimeSeriesTestVisualizer.class);
  private final SingleActionAwait singleActionAwait = new SingleActionAwait();
  private TestVisualizationWindow visualizer;
  private Point lastLocation;

  private String errorMessage;

  @Override
  public boolean visualizeBlocking(TsdlTestInfo testInformation, TsdlTestVisualization visualizationConfiguration) {
    if (visualizationConfiguration != null && visualizationConfiguration.skipVisualization()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Skipping visualization of test '{}' because the 'skipVisualization' parameter of the @{}' annotation is true.",
            testInformation.longName(), TsdlTestVisualization.class.getSimpleName());
      }
      return true;
    }

    singleActionAwait.reset();
    var dataSet = createDataset(testInformation.timeSeries());
    SwingUtilities.invokeLater(() -> {
      try {
        visualizer = new TestVisualizationWindow(testInformation, visualizationConfiguration, dataSet, singleActionAwait, lastLocation);
      } catch (Exception e) {
        errorMessage = e.getMessage();
        singleActionAwait.actionPerformed();
      }
    });

    // block until UI interaction is done, i.e. CountDownLatch has reached zero
    singleActionAwait.waitFor();
    lastLocation = visualizer.locationOnClose;

    if (errorMessage != null) {
      LOGGER.warn("Encountered problem while visualizing test: {}", errorMessage);
      LOGGER.warn("Executing test anyway, without prior visualization.");
      return true;
    }

    return visualizer.shouldExecuteTest;
  }

  private XYDataset createDataset(List<List<DataPoint>> timeSeries) {
    var dataset = new TimeSeriesCollection();

    for (var i = 0; i < timeSeries.size(); i++) {
      var series = timeSeries.get(i);

      var newSeries = new TimeSeries(String.format("Series%d", i));
      for (var dataPoint : series) {
        newSeries.add(new Millisecond(Date.from(dataPoint.getTimestamp())), dataPoint.asDecimal());
      }

      dataset.addSeries(newSeries);
    }

    return dataset;
  }

  private static class TestVisualizationWindow extends JFrame {
    private Boolean shouldExecuteTest;
    private Point locationOnClose;

    public TestVisualizationWindow(TsdlTestInfo testInfo,
                                   TsdlTestVisualization visualizationConfig,
                                   XYDataset data,
                                   SingleActionAwait clickAwaiter,
                                   Point startUpLocation) {
      super(testInfo.longName());

      getContentPane().add(createChartPanel(testInfo.shortName(), visualizationConfig, data), BorderLayout.CENTER);
      getContentPane().add(createButtonPanel(clickAwaiter), BorderLayout.SOUTH);

      setAlwaysOnTop(true);
      setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

      pack();
      if (startUpLocation != null) {
        setLocation(startUpLocation);
      } else {
        setLocationRelativeTo(null); // centers frame
      }
      setVisible(true);
    }

    private JPanel createChartPanel(String title, TsdlTestVisualization visualizationInfo, XYDataset data) {
      var chart = ChartFactory.createTimeSeriesChart(title, "Date Time", "Value", data);
      var plot = (XYPlot) chart.getPlot();

      var renderPointShapes = visualizationInfo == null || visualizationInfo.renderPointShape();
      var renderer = (XYLineAndShapeRenderer) plot.getRenderer();
      for (var i = 0; i < plot.getSeriesCount(); i++) {
        renderer.setSeriesShapesVisible(i, renderPointShapes);
      }

      plot.setBackgroundPaint(new Color(255, 255, 255));
      var domain = (DateAxis) plot.getDomainAxis();

      if (visualizationInfo != null && !TsdlTestVisualization.DEFAULT_AXIS_FORMAT.equals(visualizationInfo.dateAxisFormat())) {
        domain.setDateFormatOverride(new SimpleDateFormat(visualizationInfo.dateAxisFormat()));
      }

      return new ChartPanel(chart);
    }

    private JPanel createButtonPanel(SingleActionAwait clickAwaiter) {
      var btnGet = new JButton("Run Test");
      var btnNo = new JButton("Skip Test");

      // both buttons should close the window
      ActionListener frameCloser = e -> {
        locationOnClose = getLocation();
        TestVisualizationWindow.this.dispose();
      };
      btnGet.addActionListener(frameCloser);
      btnNo.addActionListener(frameCloser);

      // execution is blocked until one of the buttons has been clicked
      btnGet.addActionListener(clickAwaiter);
      btnNo.addActionListener(clickAwaiter);

      // only one button denotes that the test should be executed
      btnGet.addActionListener(e1 -> shouldExecuteTest = true);
      btnNo.addActionListener(e -> shouldExecuteTest = false);

      var btnPanel = new JPanel(new GridLayout(1, 2));
      btnPanel.add(btnGet);
      btnPanel.add(btnNo);

      return btnPanel;
    }
  }

  private static class SingleActionAwait implements ActionListener {
    private CountDownLatch latch = createCountDown();

    public void actionPerformed() {
      actionPerformed(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      latch.countDown();
    }

    public void waitFor() {
      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    private CountDownLatch createCountDown() {
      return new CountDownLatch(1);
    }

    public void reset() {
      latch = createCountDown();
    }
  }
}

