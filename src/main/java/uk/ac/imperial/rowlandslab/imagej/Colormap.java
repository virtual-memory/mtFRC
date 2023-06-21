package uk.ac.imperial.rowlandslab.imagej;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

public class Colormap
{
	public static BufferedImage generateColormap(FRCResult[][] frcResultMatrix, int numTilesHorizontal, int numTilesVertical)
	{
		// Convert FRCResult matrix to double matrix

		double[][] frcMatrix = new double[numTilesVertical][numTilesHorizontal];

		for( int v=0 ; v < frcResultMatrix.length ; v++ )
		{
			for( int h=0 ; h < frcResultMatrix[v].length ; h++ )
			{
				if( frcResultMatrix[v][h] == null )
				{
					continue;
				}

				frcMatrix[v][h] = frcResultMatrix[v][h].frc;
			}
		}

		return generateColormap(frcMatrix, numTilesHorizontal, numTilesVertical);
	}


	public static BufferedImage generateColormap(double[][] frcMatrix, int numTilesHorizontal, int numTilesVertical)
	{
		// Normalise array
		double resolutionCutoff = 33; // Change this value to normalise as required
		double[][] fireMatrixNormalised = normaliseIntArrayWithDynamicRange(frcMatrix, resolutionCutoff);

		// Convert double array to int array
		int[][] fireMatrixInt = doubleArrayToIntArray(fireMatrixNormalised);

		// Flatten array
		int[] fireMatrixIntFlat = Arrays.stream(fireMatrixInt).flatMapToInt(Arrays::stream).toArray();

		// Convert array to colormap image
		BufferedImage colormap = arrayToImage(fireMatrixIntFlat, numTilesHorizontal, numTilesVertical);

		return colormap;
	}

	private static BufferedImage arrayToImage(int[] pixels, int width, int height)
	{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

		WritableRaster wr = image.getRaster();
		wr.setPixels(0, 0, width, height, pixels);

		image.setData(wr); // TEST

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

	private static double[][] normaliseIntArrayWithDynamicRange(double[][] originalArray, double resolutionCutoff)
	{
		double normalisedMax = 255;

		double[][] normalisedArray = new double[originalArray.length][originalArray[0].length];

		for (int i = 0; i < originalArray.length; i++)
		{
			for (int j = 0; j < originalArray[0].length; j++)
			{
				// Set to normalised max if resolution greater than cutoff
				if (originalArray[i][j] > resolutionCutoff)
				{
					normalisedArray[i][j] = normalisedMax;
					continue;
				}

				// Set to normalised max if value is -1 (meaning 'no correlations')
				if (originalArray[i][j] < 0)
				{
					normalisedArray[i][j] = normalisedMax;
					continue;
				}

				double normalisedVal = (originalArray[i][j] / resolutionCutoff) * normalisedMax;
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
