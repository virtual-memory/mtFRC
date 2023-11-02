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
	public static void batchMeanTiledFRC(boolean useSingleImageAnalysis, boolean useRois, int tileWidth, int tileHeight, double originalImagePixelSize, double stepSize, boolean isShowPlotEnabled, boolean isGenerateColormapsEnabled, double colormapUmMin, double colormapUmMax)
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
			int numSlices = image.getNSlices();

			rois = new Roi[numSlices];

			//int nChannels = ip.getNChannels();
			int channel = 1;
			int frame = 1;

			for (int sliceIndex = 1; sliceIndex <= numSlices; sliceIndex++)
			{
				Roi roi = new Roi(0, 0, image.getWidth(), image.getHeight());

//				int position = ((sliceIndex - 1) * nChannels) + channel;
//				roi.setPosition(position);
				roi.setPosition(channel, sliceIndex, frame);

				roi.setName("Full Frame " + sliceIndex);
				rois[sliceIndex-1] = roi;
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
					int slicePosition = roi.getZPosition();

					// If slice not set, set to 1
					if (slicePosition == 0)
					{
						slicePosition = 1;
					}


					// Run mtFRC and add result to list
					TiledFRCResult result;
					if( useSingleImageAnalysis )
					{
						// Get image processor for slice
						int nChannels = ip.getNChannels();
						int slice = slicePosition;
						int channel = 1;
						int position = ((slice - 1) * nChannels) + channel;
						ImageProcessor sliceIp = ip.getStack().getProcessor(position);

						// Get mask
						ImageProcessor maskIp = ImageUtils.roiToMask(roi, sliceIp).getProcessor();

						result = MeanTiledFRC.runMeanTiledFRCSingleImage(sliceIp, imageType, maskIp, roi.getName(), slicePosition, tileWidth, tileHeight);
					}
					else
					{
						// Get image processor for slice from channel 1
						int nChannels = ip.getNChannels();
						int slice = slicePosition;
						int channel = 1;
						int position = ((slice - 1) * nChannels) + channel;
						ImageProcessor sliceIp1 = ip.getStack().getProcessor(position);

						// Get image processor for slice from channel 2
						channel = 2;
						position = ((slice - 1) * nChannels) + channel;
						ImageProcessor sliceIp2 = ip.getStack().getProcessor(position);

						// Get mask
						ImageProcessor maskIp = ImageUtils.roiToMask(roi, sliceIp1).getProcessor();

						result = MeanTiledFRC.runMeanTiledFRCTwoImage(sliceIp1, sliceIp2, imageType, maskIp, roi.getName(), slicePosition, tileWidth, tileHeight);
					}

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

				showResultsTable(useSingleImageAnalysis, results, originalImagePixelSize, stepSize);

				if(isShowPlotEnabled)
				{
					// If only a single frame was analysed, show a message instead of the plot
					if( results.size() < 2 )
					{
						IJ.log("Plot not displayed as only one result.");
					}
					else
					{
						showResultsPlot(useSingleImageAnalysis, results, originalImagePixelSize, stepSize);
					}
				}

				if(isGenerateColormapsEnabled)
				{
					int numTilesHorizontal = ip.getWidth() / tileWidth;
					int numTilesVertical = ip.getHeight() / tileHeight;

					int colormapWidth = ip.getWidth();
					int colormapHeight = ip.getHeight();

					if( useSingleImageAnalysis )
					{
						numTilesHorizontal /= 2;
						numTilesVertical /= 2;

						colormapWidth /= 2;
						colormapHeight /= 2;
					}

					generateColormaps(useSingleImageAnalysis, results, numTilesHorizontal, numTilesVertical, colormapWidth, colormapHeight, colormapUmMin, colormapUmMax, originalImagePixelSize);
				}
			}
		};

		worker.execute();
	}

	private static void showResultsTable(boolean useSingleImageAnalysis, ArrayList<TiledFRCResult> results, double originalImagePixelSize, double stepSize)
	{
		// Convert pixel size to sub-image pixel size if using Single Image analysis
		double appliedImagePixelSize = originalImagePixelSize;
		if( useSingleImageAnalysis )
		{
			appliedImagePixelSize *= 2;
		}

		ResultsTable rt = Analyzer.getResultsTable();
		if (rt == null)
		{
			rt = new ResultsTable();
			Analyzer.setResultsTable(rt);
		}

		for( TiledFRCResult result : results )
		{
			double depth = result.slicePosition * stepSize;

			double mtFRCPixels = result.meanTiledFrc;

			// For Single Image analysis, convert 'sub-image pixels' to 'original image pixels'
			if( useSingleImageAnalysis )
			{
				mtFRCPixels *= 2;
			}

			double mtFrcUm = result.meanTiledFrc * appliedImagePixelSize;

			rt.incrementCounter();
			rt.addValue("ROI", result.name);
			rt.addValue("Depth", depth);
			rt.addValue("mtFRC (Pixels)", mtFRCPixels);
			rt.addValue("mtFRC (μm)", mtFrcUm);
			rt.addValue("% unresolved", result.percentageUndetectedResolutionTiles);
		}

		rt.show("Results");
	}

	private static void showResultsPlot(boolean useSingleImageAnalysis, ArrayList<TiledFRCResult> results, double originalImagePixelSize, double stepSize)
	{
		// Convert pixel size to sub-image pixel size if using Single Image analysis
		double appliedImagePixelSize = originalImagePixelSize;
		if( useSingleImageAnalysis )
		{
			appliedImagePixelSize *= 2;
		}

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

			double mtFrcUm = result.meanTiledFrc * appliedImagePixelSize;
			yVals.add(mtFrcUm);
		}

		double[] x = xVals.stream().mapToDouble(i->i).toArray();
		double[] y = yVals.stream().mapToDouble(i->i).toArray();

		Plot plot = new Plot("mtFRC Plot", "Depth", "mtFRC", x, y);
		plot.show();
	}

	private static void generateColormaps(boolean useSingleImageAnalysis, ArrayList<TiledFRCResult> results, int numTilesHorizontal, int numTilesVertical, int colormapWidth, int colormapHeight, double colormapUmMin, double colormapUmMax, double originalImagePixelSize)
	{
		double appliedPixelSize = originalImagePixelSize;

		// Convert pixel size to sub-image pixel size if using Single Image analysis
		if( useSingleImageAnalysis )
		{
			appliedPixelSize *= 2;
		}

		// Get min and max values for entire result set
		double colorMapPixelsMin = colormapUmMin / appliedPixelSize;
		double colorMapPixelsMax = colormapUmMax / appliedPixelSize;

		ImageStack stack = new ImageStack(colormapWidth, colormapHeight);

		for( TiledFRCResult result : results )
		{
			// Generate colormap
			double[][] frcMatrix = FRCResult.convertFrcResultMatrixToDoubleMatrix(result.frcArray);
			ImagePlus colormap = Colormap.generateColormap(frcMatrix, numTilesHorizontal, numTilesVertical, colorMapPixelsMin, colorMapPixelsMax);

			// Resize colormap to size of sub-image (Single Image analysis) or original image (Two Image analysis)
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
