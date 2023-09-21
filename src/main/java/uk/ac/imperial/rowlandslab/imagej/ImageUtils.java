package uk.ac.imperial.rowlandslab.imagej;


import ij.gui.Roi;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;


public class ImageUtils
{
	public static String getImageType(ImagePlus imagePlus)
	{
		int type = imagePlus.getType();

		switch (type)
		{
			case ImagePlus.GRAY8:
				return "8-bit grayscale";
			case ImagePlus.GRAY16:
				return "16-bit grayscale";
			case ImagePlus.GRAY32:
				return "32-bit grayscale";
			case ImagePlus.COLOR_256:
				return "8-bit color";
			case ImagePlus.COLOR_RGB:
				return "RGB color";
			default:
				return "Unknown type";
		}
	}
	public static boolean[][] getMaskArray(BufferedImage bufferedImage)
	{
		WritableRaster raster = bufferedImage.getRaster();
		boolean[][] arr = new boolean[raster.getHeight()][raster.getWidth()];

		for (int v = 0; v < raster.getHeight(); v++)
		{
			for (int h = 0; h < raster.getWidth(); h++)
			{
				int pixel = raster.getSample(h, v, 0);

				if( pixel > 0 )
				{
					arr[v][h] = true;
				}
				else
				{
					arr[v][h] = false;
				}
			}
		}

		return arr;
	}

	public static ImagePlus generateTile(ImagePlus image, int startX, int startY, int tileWidth, int tileHeight)
	{
		ImageProcessor ip = image.getProcessor();

		ip.setRoi(startX, startY, tileWidth, tileHeight);
		ImageProcessor croppedIp = ip.crop();

		ImagePlus imageCropped = new ImagePlus("tile", croppedIp);

		return imageCropped;
	}
	public static BufferedImage resizeImage(ImageProcessor ip, int targetWidth, int targetHeight, int imageType)
	{
		BufferedImage inputImage = ip.getBufferedImage();
		return resizeImage(inputImage, targetWidth, targetHeight, imageType);
	}

	public static BufferedImage resizeImage(BufferedImage inputImage, int targetWidth, int targetHeight, int imageType)
	{
		Image scaledImage = inputImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
		BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, imageType);
		outputImage.getGraphics().drawImage(scaledImage, 0, 0, null);
		return outputImage;
	}

	public static ImagePlus roiToMask(Roi roi, ImageProcessor sliceIp)
	{
		// Set ROI on the current slice
		sliceIp.setRoi(roi);

		// Get mask for the ROI
		ImageProcessor mask = sliceIp.getMask();
		ImageProcessor ip2 = new ByteProcessor(sliceIp.getWidth(), sliceIp.getHeight());
		ip2.setColor(0);
		ip2.fill();

		// Handle case where mask is a rectangle
		if (mask == null)
		{
			mask = new ByteProcessor(roi.getBounds().width, roi.getBounds().height);
			mask.setColor(255);
			mask.fill();
		}

		ip2.insert(mask, roi.getBounds().x, roi.getBounds().y);

		// Create a new ImagePlus object for the mask
		ImagePlus roiImage = new ImagePlus("Mask", ip2);

		return roiImage;
	}

	public static ImagePlus[] splitImage(ImageProcessor ip, String imageType)
	{
		// Get image dimensions
		int imgWidth = ip.getWidth();
		int imgHeight = ip.getHeight();

		// If width or height is an odd number, ignore the last row/column of pixels
		if( imgWidth % 2 == 1 )
		{
			imgWidth -= 1;
		}
		if( imgHeight % 2 == 1 )
		{
			imgHeight -= 1;
		}

		ImagePlus split1 = IJ.createImage("Split 1", imageType, imgWidth/2, imgHeight/2, 1);
		ImagePlus split2 = IJ.createImage("Split 2", imageType, imgWidth/2, imgHeight/2, 1);
		ImagePlus split3 = IJ.createImage("Split 3", imageType, imgWidth/2, imgHeight/2, 1);
		ImagePlus split4 = IJ.createImage("Split 4", imageType, imgWidth/2, imgHeight/2, 1);

		ImageProcessor split1Ip = split1.getProcessor();
		ImageProcessor split2Ip = split2.getProcessor();
		ImageProcessor split3Ip = split3.getProcessor();
		ImageProcessor split4Ip = split4.getProcessor();

		for(int y = 0 ; y < imgHeight ; y++)
		{
			for (int x = 0; x < imgWidth; x++)
			{
				int pixelVal = ip.get(x, y);

				int splitX = x/2;
				int splitY = y/2;

				if( (x % 2 == 0 && y % 2 == 0) ) // Even-even
				{
					split1Ip.set(splitX, splitY, pixelVal);
				}
				else if( (x % 2 == 1 && y % 2 == 1) ) // Odd-odd
				{
					split2Ip.set(splitX, splitY, pixelVal);
				}
				else if( (x % 2 == 0 && y % 2 == 1) ) // Even-odd
				{
					split3Ip.set(splitX, splitY, pixelVal);
				}
				else if( (x % 2 == 1 && y % 2 == 0) ) // Odd-even
				{
					split4Ip.set(splitX, splitY, pixelVal);
				}
			}
		}

		ImagePlus[] imgArray = new ImagePlus[4];
		imgArray[0] = split1;
		imgArray[1] = split2;
		imgArray[2] = split3;
		imgArray[3] = split4;

		return imgArray;
	}
}
