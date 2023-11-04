package uk.ac.imperial.rowlandslab.imagej;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

public class Colormap
{
	public static ImagePlus generateColormap(double[][] frcMatrix, int numTilesHorizontal, int numTilesVertical, double minVal, double maxVal)
	{
		// Normalise array
		double[][] frcMatrixNormalised = normaliseArray(frcMatrix, minVal, maxVal);

		// Convert double array to int array
		int[][] frcMatrixInt = doubleArrayToIntArray(frcMatrixNormalised);

		// Flatten array
		int[] frcMatrixIntFlat = Arrays.stream(frcMatrixInt).flatMapToInt(Arrays::stream).toArray();

		// Convert array to colormap image
		ImagePlus colormap = arrayToImage(frcMatrixIntFlat, numTilesHorizontal, numTilesVertical);

		return colormap;
	}

	private static ImagePlus arrayToImage(int[] pixels, int width, int height)
	{
		if (width <= 0 || height <= 0 || pixels == null || pixels.length != width * height)
		{
			throw new IllegalArgumentException("arrayToImage: Invalid input parameters.");
		}

		// Create a new black ImagePlus with 16-bit grayscale.
		ImagePlus image = NewImage.createImage("image", width, height, 1, 16, NewImage.FILL_BLACK);

		// Get the ImageProcessor from the ImagePlus.
		ImageProcessor ip = image.getProcessor();

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int pixelValue = pixels[y * width + x];
				ip.putPixelValue(x, y, pixelValue);
			}
		}

		return image;
	}

	private static int[][] doubleArrayToIntArray(double[][] doubleArray)
	{
		int[][] intArray = new int[doubleArray.length][doubleArray[0].length];

		for (int i = 0; i < doubleArray.length; i++)
		{
			for (int j = 0; j < doubleArray[0].length; j++)
			{
				intArray[i][j] = (int) doubleArray[i][j];
			}
		}

		return intArray;
	}

	private static double[][] normaliseArray(double[][] originalArray, double minVal, double maxVal)
	{
		double normalisedMin = 0;
		double normalisedMax = 65535; // For 16-bit images
		double scaleFactor = (normalisedMax - normalisedMin) / (maxVal - minVal);

		double[][] normalisedArray = new double[originalArray.length][originalArray[0].length];

		for (int i = 0; i < originalArray.length; i++)
		{
			for (int j = 0; j < originalArray[0].length; j++)
			{
				// Set to normalised max if value is -1 (meaning 'no correlations')
				if (originalArray[i][j] < 0)
				{
					normalisedArray[i][j] = normalisedMax;
					continue;
				}

				double normalisedVal = (originalArray[i][j] - minVal) * scaleFactor;

				// Limit normalised value if less than the minimum
				if( normalisedVal < normalisedMin )
				{
					normalisedVal = normalisedMin;
				}

				// Limit normalised value if greater than the maximum
				if( normalisedVal > normalisedMax )
				{
					normalisedVal = normalisedMax;
				}

				normalisedArray[i][j] = normalisedVal;
			}
		}

		return normalisedArray;
	}
}
