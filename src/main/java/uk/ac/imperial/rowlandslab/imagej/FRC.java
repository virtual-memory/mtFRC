package uk.ac.imperial.rowlandslab.imagej;


import java.util.ArrayList;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import org.apache.commons.math3.complex.Complex;


public class FRC
{
	public static FRCResult runFRC(ImagePlus image1, ImagePlus image2)
	{
		ImageProcessor ip1 = image1.getProcessor();
		ImageProcessor ip2 = image2.getProcessor();

		// Get image dimensions
		int imgWidth = ip1.getWidth();
		int imgHeight = ip1.getHeight();

		int imgCenterX = imgWidth/2;
		int imgCenterY = imgHeight/2;

		int maxRadius = imgWidth/2;

		// For each FFT, create an array of ArrayLists to store complex values.
		// There is one ArrayList for each possible 'radius'.
		ArrayList<Complex>[] dataList1 = new ArrayList[maxRadius+1];
		ArrayList<Complex>[] dataList2 = new ArrayList[maxRadius+1];

		// Initialise ArrayLists
		for(int r=0 ; r < maxRadius+1 ; r++)
		{
			dataList1[r] = new ArrayList<Complex>();
			dataList2[r] = new ArrayList<Complex>();
		}

		// Compute complex FFTs
		FloatProcessor[] fp1 = getFFT(ip1);
		FloatProcessor[] fp2 = getFFT(ip2);

		// Loop through all pixels in both FFTs.
		// For each pixel, add its complex value to the ArrayList for the corresponding radius.
		for(int y = 0 ; y < imgHeight ; y++)
		{
			for(int x = 0 ; x < imgWidth ; x++ )
			{
				// Use Pythagorean theorem to calculate 'radius' (distance from image center) of current pixel.
				int radius = (int) Math.round(Math.hypot(x-imgCenterX, y-imgCenterY));

				// Skip the pixel if the radius is beyond the maximum
				if( radius > maxRadius )
				{
					continue;
				}

				// Get complex value at that pixel in each FFT
				Complex complex1 = new Complex(fp1[0].getf(x, y), fp1[1].getf(x, y));
				Complex complex2 = new Complex(fp2[0].getf(x, y), fp2[1].getf(x, y));

				dataList1[radius].add(complex1);
				dataList2[radius].add(complex2);
			}
		}

		// Initialise variable to store FRC curve
		double[] frcCurve = new double[maxRadius+1];

		// Calculate complex Pearson correlation coefficient for each radius
		for(int r=1 ; r < maxRadius+1 ; r++)
		{
			double corr = complexCorrelation(dataList1[r], dataList2[r]);
			frcCurve[r] = corr;
		}

		// Determine cutoff frequency

		double cutoff = 1f/7f;
		int cutoffRadius = 0;
		FRCResultType resultType = FRCResultType.NO_CORRELATIONS;

		for(int r=1 ; r < maxRadius+1 ; r++)
		{
			if( frcCurve[r] < cutoff )
			{
				if(r > 1)
				{
					resultType = FRCResultType.HAS_CUTOFF;
				}

				break;
			}

			cutoffRadius = r;

			if( r == maxRadius )
			{
				resultType = FRCResultType.ALL_CORRELATIONS;
			}
		}

		if(resultType == FRCResultType.NO_CORRELATIONS)
		{
			return new FRCResult(FRCResultType.NO_CORRELATIONS, 999999);
		}

		double cutoffFreq = 0.5*( (double)cutoffRadius/((double)imgWidth/2));
		double fireVal = 1/cutoffFreq;

		return new FRCResult(resultType, fireVal);
	}

	private static FloatProcessor[] getFFT(ImageProcessor ip)
	{
		// Compute complex FFT

		FHT fht = new FHT(ip);
		fht.transform();

		ImageStack stack = fht.getComplexTransform();

		FloatProcessor[] fp = new FloatProcessor[2];
		fp[0] = ((FloatProcessor) stack.getProcessor(1));
		fp[1] = ((FloatProcessor) stack.getProcessor(2));

		return fp;
	}

	private static double complexCorrelation(ArrayList<Complex> list1, ArrayList<Complex> list2)
	{
		ArrayList<Complex> list2Conj = getListComplexConjugate(list2);

		int listLen = list1.size();

		// Compute numerator

		double sum = 0;
		for(int i=0 ; i < listLen ; i++)
		{
			Complex multiplication = list1.get(i).multiply(list2Conj.get(i));
			double real = multiplication.getReal();
			sum += real;
		}

		double numerator = sum;

		// Compute denominator

		ArrayList<Double> list1Squared = getListComplexSquared(list1);
		ArrayList<Double> list2Squared = getListComplexSquared(list2);

		double list1SquaredSum = getListSum(list1Squared);
		double list2SquaredSum = getListSum(list2Squared);

		double denominator = Math.sqrt( list1SquaredSum * list2SquaredSum );

		return numerator/denominator;
	}

	private static ArrayList<Complex> getListComplexConjugate(ArrayList<Complex> list)
	{
		// Return the list of complex values with each element transformed into its complex conjugate.

		ArrayList<Complex> listComplexConjugate = new ArrayList<Complex>();

		int listLen = list.size();
		for(int i=0 ; i < list.size() ; i++)
		{
			listComplexConjugate.add( list.get(i).conjugate() );
		}

		return listComplexConjugate;
	}

	private static ArrayList<Double> getListComplexSquared(ArrayList<Complex> list)
	{
		// Return the list of complex values with each element squared.

		ArrayList<Double> listComplexSquared = new ArrayList<Double>();

		for(int i=0 ; i < list.size() ; i++)
		{
			listComplexSquared.add( Math.pow(list.get(i).abs(), 2) );
		}

		return listComplexSquared;
	}

	private static double getListSum(ArrayList<Double> list)
	{
		double sum = 0;

		for(int i=0 ; i < list.size() ; i++)
		{
			sum += list.get(i);
		}

		return sum;
	}
}
