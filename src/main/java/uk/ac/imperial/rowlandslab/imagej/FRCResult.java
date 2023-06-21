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
}
