package uk.ac.imperial.rowlandslab.imagej;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.*;

import ij.WindowManager;
import ij.gui.NewImage;
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
	public static void batchMeanTiledFRC(boolean useRois, int tileWidth, int tileHeight, double originalImagePixelSize, double stepSize, boolean isShowPlotEnabled, boolean isGenerateColormapsEnabled, double colormapUmMin, double colormapUmMax)
	{
		// Get current image stack
		ImagePlus ip = IJ.getImage();

		Roi[] rois;
		if( useRois )
		{
			// If using ROIs, get ROIs from ROI manager

			RoiManager roiManager = RoiManager.getInstance();
			rois = roiManager.getRoisAsArray();
		}
		else
		{
			// If set to analyse whole image, generate an ROI covering the full frame for each frame in the stack

			ImagePlus image = IJ.getImage();
			int numFrames = image.getStackSize();

			rois = new Roi[numFrames];

			for (int i = 0; i < numFrames; i++)
			{
				Roi roi = new Roi(0, 0, image.getWidth(), image.getHeight());
				roi.setPosition(i+1);
				roi.setName("Full Frame " + (i+1));
				rois[i] = roi;
			}
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
					IJ.showStatus("Processing mtFRC: " + (i+1) + "/" + rois.length);
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
					TiledFRCResult result = MeanTiledFRC.runMeanTiledFRCSingleImage(sliceIp, imageType, maskIp, roi.getName(), slicePosition, tileWidth, tileHeight);
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
					IJ.error("Error getting results: " + e.getMessage());
					return;
				}

				IJ.showStatus("");
				IJ.showProgress(1.0);

				showResultsTable(results, originalImagePixelSize, stepSize);

				if(isShowPlotEnabled)
				{
					// If only a single frame was analysed, show a message instead of the plot
					if( results.size() < 2 )
					{
						IJ.log("Plot not displayed as only one result.");
					}
					else
					{
						showResultsPlot(results, originalImagePixelSize, stepSize);
					}
				}

				if(isGenerateColormapsEnabled)
				{
					int numTilesHorizontal = ip.getWidth() / (2 * tileWidth);
					int numTilesVertical = ip.getHeight() / (2 * tileHeight);

					int colormapWidth = ip.getWidth() / 2;
					int colormapHeight = ip.getHeight() / 2;

					generateColormaps(results, numTilesHorizontal, numTilesVertical, colormapWidth, colormapHeight, colormapUmMin, colormapUmMax, originalImagePixelSize);
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

	private static void generateColormaps(ArrayList<TiledFRCResult> results, int numTilesHorizontal, int numTilesVertical, int colormapWidth, int colormapHeight, double colormapUmMin, double colormapUmMax, double originalImagePixelSize)
	{
		// Get min and max values for entire result set
		double subImagePixelSize = originalImagePixelSize * 2;
		double colorMapPixelsMin = colormapUmMin / subImagePixelSize;
		double colorMapPixelsMax = colormapUmMax / subImagePixelSize;

		ImageStack stack = new ImageStack(colormapWidth, colormapHeight);

		for( TiledFRCResult result : results )
		{
			// Generate colormap
			double[][] frcMatrix = FRCResult.convertFrcResultMatrixToDoubleMatrix(result.frcArray);
			ImagePlus colormap = Colormap.generateColormap(frcMatrix, numTilesHorizontal, numTilesVertical, colorMapPixelsMin, colorMapPixelsMax);

			// Resize colormap to size of sub-image
			ImageProcessor ip = colormap.getProcessor();
			ip = ip.resize(colormapWidth, colormapHeight);
			ImagePlus resizedColormap = new ImagePlus("Resized Colormap", ip);

			// Add to stack
			stack.addSlice(resizedColormap.getProcessor());
		}

		// Create an ImagePlus from ImageStack
		ImagePlus imagePlus = new ImagePlus("Colormaps", stack);

		// Set the current slice to the first one
		imagePlus.setSlice(1);

		// Show ImagePlus
		imagePlus.show();

		// Apply LUT
		String lut = "Fire";
		IJ.run(imagePlus, lut, "");

		generateColorScaleBar(lut, colormapUmMin, colormapUmMax);
	}

	private static void generateColorScaleBar(String lutName, double min, double max)
	{
		int width = 256;
		int height = 50;

		ImagePlus colorBarImage = NewImage.createImage("Scale", width, height, 1, 32, NewImage.FILL_BLACK);

		ImageProcessor ip = colorBarImage.getProcessor();

		// Generate grayscale bar
		for (int x = 0; x < width; x++)
		{
			float grayValue = (float) x / (width - 1);
			for (int y = 25; y < 35; y++)
			{
				ip.putPixelValue(x, y, grayValue);
			}
		}

		// Display numerical values
		Font font = new Font("SansSerif", Font.PLAIN, 14);
		ip.setFont(font);
		ip.setColor(Color.white);
		String leftString = min > 0 ? "≤" + String.valueOf(min) : String.valueOf(min);
		String rightString = "≥" + String.valueOf(max);
		ip.drawString(leftString, 10, 20);
		ip.drawString(rightString, width - 40, 20);

		// Show the image
		colorBarImage.show();

		// Show the image
		colorBarImage.show();

		// Apply LUT
		IJ.run(colorBarImage, lutName, "");
	}

}
