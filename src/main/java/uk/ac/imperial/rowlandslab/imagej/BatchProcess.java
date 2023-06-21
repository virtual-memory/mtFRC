package uk.ac.imperial.rowlandslab.imagej;


import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.*;

import ij.gui.Plot;
import ij.gui.Roi;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;


public class BatchProcess
{
	public static void batchMeanTiledFRC(int tileWidth, int tileHeight, double originalImagePixelSize, double stepSize, boolean isShowPlotEnabled, boolean isGenerateColormapsEnabled)
	{
		// Get current image stack
		ImagePlus ip = IJ.getImage();

		// Check ROI manager is open and at least one ROI added
		RoiManager roiManager = RoiManager.getInstance();
		if (roiManager == null)
		{
			IJ.error("ROI Manager is not open");
			return;
		}

		Roi[] rois = roiManager.getRoisAsArray();
		if (rois.length == 0)
		{
			IJ.error("No ROIs in ROI Manager");
			return;
		}

		// Execute mtFRC processing in a separate thread so the progress bar can be updated incrementally
		SwingWorker<ArrayList<TiledFRCResult>, Void> worker = new SwingWorker<ArrayList<TiledFRCResult>, Void>()
		{
			@Override
			protected ArrayList<TiledFRCResult> doInBackground()
			{
				String imageType = ImageUtils.getImageType(ip);

				ArrayList<TiledFRCResult> results = new ArrayList<>();

				// Loop through all ROIs
				for(int i=0 ; i < rois.length ; i++)
				{
					// Update progress bar
					IJ.showStatus("Processing mtFRC: ROI " + (i+1));
					IJ.showProgress(i, rois.length);

					// Processing logic

					Roi roi = rois[i];

					// Get the associated slice position for the ROI
					int slicePosition = roi.getPosition();

					// If slice not set, set to 1
					if (slicePosition == 0)
					{
						slicePosition = 1;
					}

					// Get slice
					ImageProcessor sliceIp = ip.getStack().getProcessor(slicePosition);

					// Get mask
					ImageProcessor maskIp = ImageUtils.roiToMask(roi, sliceIp).getProcessor();

					// Run mtFRC and add result to list
					TiledFRCResult result = MeanTiledFRC.runMeanTiledFRCSingleImage(sliceIp, imageType, maskIp, roi.getName(), slicePosition, tileWidth, tileHeight, originalImagePixelSize);
					results.add(result);

					try
					{
						Thread.sleep(90);
					}
					catch (InterruptedException e)
					{
						// Restore the interrupted status
						Thread.currentThread().interrupt();
						// Log the interruption and return
						IJ.log("Processing was interrupted");
						return null;
					}
				}
				return results;
			}

			@Override
			protected void done()
			{
				ArrayList<TiledFRCResult> results;

				try
				{
					results = get();
				}
				catch (Exception e)
				{
					IJ.error("Error getting results.");
					return;
				}

				IJ.showStatus("");
				IJ.showProgress(1.0);

				showResultsTable(results, originalImagePixelSize, stepSize);

				if(isShowPlotEnabled)
				{
					showResultsPlot(results, originalImagePixelSize, stepSize);
				}

				if(isGenerateColormapsEnabled)
				{
					int numTilesHorizontal = ip.getWidth() / (2 * tileWidth);
					int numTilesVertical = ip.getHeight() / (2 * tileHeight);

					int colormapWidth = ip.getWidth() / 2;
					int colormapHeight = ip.getHeight() / 2;

					generateColormaps(results, numTilesHorizontal, numTilesVertical, colormapWidth, colormapHeight);
				}
			}
		};

		worker.execute();
	}

	private static void showResultsTable(ArrayList<TiledFRCResult> results, double originalImagePixelSize, double stepSize)
	{
		double subImagePixelSize = originalImagePixelSize * 2;

		ResultsTable rt = Analyzer.getResultsTable();
		if (rt == null)
		{
			rt = new ResultsTable();
			Analyzer.setResultsTable(rt);
		}

		for( TiledFRCResult result : results )
		{
			double depth = result.slicePosition * stepSize;
			double mtFRCPixels = result.meanTiledFrc * 2; // Convert 'sub-image pixels' to 'original image pixels'
			double mtFrcUm = result.meanTiledFrc * subImagePixelSize;

			rt.incrementCounter();
			rt.addValue("ROI", result.name);
			rt.addValue("Depth", depth);
			rt.addValue("mtFRC (Pixels)", mtFRCPixels);
			rt.addValue("mtFRC (μm)", mtFrcUm);
			rt.addValue("% unresolved", result.percentageUndetectedResolutionTiles);
		}

		rt.show("Results");
	}

	private static void showResultsPlot(ArrayList<TiledFRCResult> results, double originalImagePixelSize, double stepSize)
	{
		double subImagePixelSize = originalImagePixelSize * 2;

		// Sort results according to slice position
		Collections.sort(results, new Comparator<TiledFRCResult>()
		{
			@Override
			public int compare(TiledFRCResult r1, TiledFRCResult r2)
			{
				return Integer.compare(r1.slicePosition, r2.slicePosition);
			}
		});

		// Construct x and y values
		int prevSlicePosition = 0;
		ArrayList<Double> xVals = new ArrayList<>();
		ArrayList<Double> yVals = new ArrayList<>();
		for( TiledFRCResult result : results )
		{
			// Skip if slice position already used and show a warning
			if( result.slicePosition == prevSlicePosition )
			{
				IJ.log("mtFRC Plot: Skipping ROI [" + result.name + "] because an ROI for this slice position already exists.");
				continue;
			}

			prevSlicePosition = result.slicePosition;

			double depth = result.slicePosition * stepSize;
			xVals.add(depth);

			double mtFrcUm = result.meanTiledFrc * subImagePixelSize;
			yVals.add(mtFrcUm);
		}

		double[] x = xVals.stream().mapToDouble(i->i).toArray();
		double[] y = yVals.stream().mapToDouble(i->i).toArray();

		Plot plot = new Plot("mtFRC Plot", "Depth", "mtFRC", x, y);
		plot.show();
	}

	private static void generateColormaps(ArrayList<TiledFRCResult> results, int numTilesHorizontal, int numTilesVertical, int colormapWidth, int colormapHeight)
	{
		ImageStack stack = new ImageStack(colormapWidth, colormapHeight);

		for( TiledFRCResult result : results )
		{
			// Generate colormap
			BufferedImage colormap = Colormap.generateColormap(result.frcArray, numTilesHorizontal, numTilesVertical);

			// Resize colormap to size of sub-image
			BufferedImage resizedColormap = ImageUtils.resizeImage(colormap, colormapWidth, colormapHeight);

			// Add to stack
			ImagePlus imagePlus = new ImagePlus("Colormap", resizedColormap);
			stack.addSlice(imagePlus.getProcessor());
		}

		// Create an ImagePlus from ImageStack
		ImagePlus imagePlus = new ImagePlus("Colormaps", stack);

		// Set the current slice to the first one
		imagePlus.setSlice(1);

		// Show ImagePlus
		imagePlus.show();

		// Apply LUT
		IJ.run(imagePlus, "Fire", "");
	}
}
