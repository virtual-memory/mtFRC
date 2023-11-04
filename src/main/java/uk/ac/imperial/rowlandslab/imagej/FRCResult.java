package uk.ac.imperial.rowlandslab.imagej;


enum FRCResultType
{
	NO_CORRELATIONS,
	ALL_CORRELATIONS,
	HAS_CUTOFF
}


public class FRCResult
{
	public FRCResultType type;
	double frc;

	public FRCResult(FRCResultType type, double frc)
	{
		this.type = type;
		this.frc = frc;
	}

	public boolean noCorrelations()
	{
		return (this.type == FRCResultType.NO_CORRELATIONS);
	}

	public boolean allCorrelations()
	{
		return (this.type == FRCResultType.ALL_CORRELATIONS);
	}

	public void printResult()
	{
		switch (this.type)
		{
			case NO_CORRELATIONS:
				System.out.println("No correlations");
				break;
			case HAS_CUTOFF:
				System.out.println("Has cutoff: " + this.frc);
				break;
			case ALL_CORRELATIONS:
				System.out.println("All correlations");
				break;
		}
	}

	public static double[][] convertFrcResultMatrixToDoubleMatrix(FRCResult[][] frcResultMatrix)
	{
		int numTilesVertical = frcResultMatrix.length;
		int numTilesHorizontal = frcResultMatrix[0].length;

		double[][] frcMatrix = new double[numTilesVertical][numTilesHorizontal];

		for( int v=0 ; v < numTilesVertical ; v++ )
		{
			for( int h=0 ; h < numTilesHorizontal ; h++ )
			{
				// Pixel not in ROI
				if( frcResultMatrix[v][h] == null )
				{
					frcMatrix[v][h] = Double.MAX_VALUE;
					continue;
				}

				frcMatrix[v][h] = frcResultMatrix[v][h].frc;
			}
		}

		return frcMatrix;
	}
}
