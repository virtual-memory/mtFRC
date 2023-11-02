package uk.ac.imperial.rowlandslab.imagej;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;

public class MeanTiledFRC
{
	public static TiledFRCResult runMeanTiledFRCSingleImage(ImageProcessor imgIp, String imageType, ImageProcessor maskIp, String name, int slicePosition, int tileWidth, int tileHeight)
	{
		// Split image into four sub-images
		ImagePlus[] imgArray = ImageUtils.splitImage(imgIp, imageType);

		// Get sub-image dimensions
		ImageProcessor ip = imgArray[0].getProcessor();
		int singleImgWidth = ip.getWidth();
		int singleImgHeight = ip.getHeight();

		// Resize mask
		BufferedImage subImageMask = ImageUtils.resizeImage(maskIp, singleImgWidth, singleImgHeight, BufferedImage.TYPE_BYTE_GRAY);

		// Get sub-image image processors
		ImageProcessor ip1 = imgArray[0].getProcessor();

		// Calculate number of tiles in each dimension
		int numTilesHorizontal = ip1.getWidth() / tileWidth;
		int numTilesVertical = ip1.getHeight() / tileHeight;

		// Convert mask to array
		boolean[][] maskArray = ImageUtils.getMaskArray(subImageMask);

		// Verify mask size matches image size
		if( ip1.getWidth() != maskArray[0].length || ip1.getHeight() != maskArray.length)
		{
			IJ.error("Error: Mask size does not match image size.");
			return null;
		}

		// Calculate tiled FRC data

		double frcSum = 0;

		// Initialise array to store results
		FRCResult[][] frcArray = new FRCResult[numTilesVertical][numTilesHorizontal];

		int numTilesInMaskArea = 0;

		int numDetectedResolutionTiles = 0;
		int numUndetectedResolutionTiles = 0;

		double minFRC = Double.MAX_VALUE; // FRC value of the tile with the lowest FRC value

		for (int v=0 ; v < numTilesVertical ; v++)
		{
			for( int h=0 ; h < numTilesHorizontal ; h++ )
			{
				int tileStartX = h * tileWidth;
				int tileStartY = v * tileHeight;

				int tileCenterX = tileStartX + (tileWidth/2);
				int tileCenterY = tileStartY + (tileHeight/2);

				// Calculate FRC value only if pixel is in the mask
				if( maskArray[tileCenterY][tileCenterX] )
				{
					// Increment 'num tiles in mask area' counter
					numTilesInMaskArea += 1;

					ImagePlus tile1 = ImageUtils.generateTile(imgArray[0].getProcessor(), tileStartX, tileStartY, tileWidth, tileHeight);
					ImagePlus tile2 = ImageUtils.generateTile(imgArray[1].getProcessor(), tileStartX, tileStartY, tileWidth, tileHeight);
					ImagePlus tile3 = ImageUtils.generateTile(imgArray[2].getProcessor(), tileStartX, tileStartY, tileWidth, tileHeight);
					ImagePlus tile4 = ImageUtils.generateTile(imgArray[3].getProcessor(), tileStartX, tileStartY, tileWidth, tileHeight);

					FRCResult result1 = FRC.runFRC(tile1, tile2);
					FRCResult result2 = FRC.runFRC(tile3, tile4);

					FRCResult averageResult = result1;
					averageResult.frc = (result1.frc + result2.frc) / 2;

					// Update 'minFRC' value if new lowest value found
					if( !result1.noCorrelations() && !result2.noCorrelations())
					{
						if( averageResult.frc < minFRC )
						{
							minFRC = averageResult.frc;
						}
					}

					// Store FRC values in array
					frcArray[v][h] = averageResult;

					if(result1.noCorrelations() || result2.noCorrelations())
					{
						numUndetectedResolutionTiles += 1;
					}
					else
					{
						// Add averaged FRC value to sum
						frcSum += averageResult.frc;

						numDetectedResolutionTiles += 1;
					}
				}
			}

		}

		// Calculate statistics
		int numTilesInImage = (numTilesHorizontal * numTilesVertical);
		double percentageTilesInMask = ((double)numTilesInMaskArea / (double)numTilesInImage)*100;
		double percentageUndetectedResolutionTiles = ((double)numUndetectedResolutionTiles / (double)numTilesInMaskArea)*100;

		// Calculate average FRC value
		double meanFrcVal = frcSum / (double)numDetectedResolutionTiles;

		return new TiledFRCResult(frcArray, meanFrcVal, name, slicePosition, numTilesInImage, numTilesInMaskArea, percentageTilesInMask, numDetectedResolutionTiles, numUndetectedResolutionTiles, percentageUndetectedResolutionTiles, minFRC);
	}

	public static TiledFRCResult runMeanTiledFRCTwoImage(ImageProcessor ip1, ImageProcessor ip2, String imageType, ImageProcessor maskIp, String name, int slicePosition, int tileWidth, int tileHeight)
	{
		// Get image dimensions
		int imgWidth = ip1.getWidth();
		int imgHeight = ip2.getHeight();

		// Calculate number of tiles in each dimension
		int numTilesHorizontal = imgWidth / tileWidth;
		int numTilesVertical = imgHeight / tileHeight;

		// Convert mask to array
		boolean[][] maskArray = ImageUtils.getMaskArray(maskIp.getBufferedImage());

		// Verify mask size matches image size
		if( ip1.getWidth() != maskArray[0].length || ip1.getHeight() != maskArray.length)
		{
			IJ.error("Error: Mask size does not match image size.");
			return null;
		}

		// Calculate tiled FRC data

		double frcSum = 0;

		// Initialise array to store results
		FRCResult[][] frcArray = new FRCResult[numTilesVertical][numTilesHorizontal];

		int numTilesInMaskArea = 0;

		int numDetectedResolutionTiles = 0;
		int numUndetectedResolutionTiles = 0;

		double minFRC = Double.MAX_VALUE; // FRC value of the tile with the lowest FRC value

		for (int v=0 ; v < numTilesVertical ; v++)
		{
			for( int h=0 ; h < numTilesHorizontal ; h++ )
			{
				int tileStartX = h * tileWidth;
				int tileStartY = v * tileHeight;

				int tileCenterX = tileStartX + (tileWidth/2);
				int tileCenterY = tileStartY + (tileHeight/2);

				// Calculate FRC value only if pixel is in the mask
				if( maskArray[tileCenterY][tileCenterX] )
				{
					// Increment 'num tiles in mask area' counter
					numTilesInMaskArea += 1;

					ImagePlus tile1 = ImageUtils.generateTile(ip1, tileStartX, tileStartY, tileWidth, tileHeight);
					ImagePlus tile2 = ImageUtils.generateTile(ip2, tileStartX, tileStartY, tileWidth, tileHeight);

					FRCResult result = FRC.runFRC(tile1, tile2);

					// Update 'minFRC' value if new lowest value found
					if( !result.noCorrelations() )
					{
						if( result.frc < minFRC )
						{
							minFRC = result.frc;
						}
					}

					// Store FRC values in array
					frcArray[v][h] = result;

					if(result.noCorrelations() || result.noCorrelations())
					{
						numUndetectedResolutionTiles += 1;
					}
					else
					{
						// Add averaged FRC value to sum
						frcSum += result.frc;

						numDetectedResolutionTiles += 1;
					}
				}
			}

		}

		// Calculate statistics
		int numTilesInImage = (numTilesHorizontal * numTilesVertical);
		double percentageTilesInMask = ((double)numTilesInMaskArea / (double)numTilesInImage)*100;
		double percentageUndetectedResolutionTiles = ((double)numUndetectedResolutionTiles / (double)numTilesInMaskArea)*100;

		// Calculate average FRC value
		double meanFrcVal = frcSum / (double)numDetectedResolutionTiles;

		return new TiledFRCResult(frcArray, meanFrcVal, name, slicePosition, numTilesInImage, numTilesInMaskArea, percentageTilesInMask, numDetectedResolutionTiles, numUndetectedResolutionTiles, percentageUndetectedResolutionTiles, minFRC);
	}
}
