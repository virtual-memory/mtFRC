package uk.ac.imperial.rowlandslab.imagej;


public class TiledFRCResult
{
	// Data
	FRCResult[][] frcArray;
	double meanTiledFrc;

	// Metadata
	String name;
	int slicePosition;

	// Statistics
	int numTilesInImage;
	int numTilesInMaskArea;
	double percentageTilesInMask;
	int numDetectedResolutionTiles;
	int numUndetectedResolutionTiles;
	double percentageUndetectedResolutionTiles;

	public TiledFRCResult(FRCResult[][] frcArray, double meanTiledFrc, String name, int slicePosition, int numTilesInImage, int numTilesInMaskArea, double percentageTilesInMask, int numDetectedResolutionTiles, int numUndetectedResolutionTiles, double percentageUndetectedResolutionTiles)
	{
		this.frcArray = frcArray;
		this.meanTiledFrc = meanTiledFrc;

		this.name = name;
		this.slicePosition = slicePosition;

		this.numTilesInImage = numTilesInImage;
		this.numTilesInMaskArea = numTilesInMaskArea;
		this.percentageTilesInMask = percentageTilesInMask;
		this.numDetectedResolutionTiles = numDetectedResolutionTiles;
		this.numUndetectedResolutionTiles = numUndetectedResolutionTiles;
		this.percentageUndetectedResolutionTiles = percentageUndetectedResolutionTiles;
	}
}
