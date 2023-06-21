package uk.ac.imperial.rowlandslab.imagej;


import ij.measure.Calibration;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.WindowManager;

import java.awt.*;

import javax.swing.*;
import java.awt.event.*;

public class MT_FRC implements PlugIn
{
	private JFrame frame;

	public void run(String arg)
	{
		SwingUtilities.invokeLater(() -> displayRunDialog());
	}

	private void displayRunDialog()
	{
		frame = new JFrame("mtFRC");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel panel = new JPanel(new GridBagLayout());
		frame.add(panel);
		GridBagConstraints c = new GridBagConstraints();

		JLabel labelPixelSize = new JLabel("Pixel Size:");
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		panel.add(labelPixelSize, c);

		double pixelSize = getCurrentImagePixelSize();

		SpinnerNumberModel modelPixelSize = new SpinnerNumberModel(pixelSize, // Initial value
				-Double.MAX_VALUE, // Min
				Double.MAX_VALUE, // Max
				1); // Step
		JSpinner spinnerPixelSize = new JSpinner(modelPixelSize);
		Dimension d = spinnerPixelSize.getPreferredSize();
		d.width = 100;
		spinnerPixelSize.setPreferredSize(d);
		c.gridx = 1;
		panel.add(spinnerPixelSize, c);

		JLabel labelStepSize = new JLabel("Step Size:");
		c.gridx = 0;
		c.gridy = 1;
		panel.add(labelStepSize, c);

		SpinnerNumberModel modelStepSize = new SpinnerNumberModel(1, // Initial value
				-Double.MAX_VALUE, // Min
				Double.MAX_VALUE, // Max
				1); // Step
		JSpinner spinnerStepSize = new JSpinner(modelStepSize);
		Dimension d2 = spinnerStepSize.getPreferredSize();
		d2.width = 100;
		spinnerStepSize.setPreferredSize(d2);
		c.gridx = 1;
		panel.add(spinnerStepSize, c);

		JCheckBox checkBoxShowPlot = new JCheckBox("Show plot");
		checkBoxShowPlot.setSelected(true);
		c.gridx = 0;
		c.gridy = 2;
		panel.add(checkBoxShowPlot, c);

		JCheckBox checkGenerateColormaps = new JCheckBox("Generate colormaps");
		c.gridy = 3;
		panel.add(checkGenerateColormaps, c);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okButtonClicked(spinnerPixelSize, spinnerStepSize, checkBoxShowPlot, checkGenerateColormaps);
			}
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelButtonClicked();
			}
		});

		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 2;
		panel.add(buttonPanel, c);

		frame.setPreferredSize(new Dimension(300, 200));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private void okButtonClicked(JSpinner spinnerPixelSize, JSpinner spinnerStepSize, JCheckBox checkBoxShowPlot, JCheckBox checkGenerateColormaps)
	{
		// Return if ROI Manager not open
		RoiManager manager = RoiManager.getInstance();
		if (manager == null)
		{
			IJ.error("To run mtFRC, please first open the ROI manager and add at least 1 ROI.");
			return;
		}

		// Return if no ROIs added
		if( manager.getRoisAsArray().length < 1 )
		{
			IJ.error("At least 1 ROI is required. Please add an ROI to the ROI manager.");
			return;
		}


		double pixelSize = (double) spinnerPixelSize.getValue();
		double stepSize = (double) spinnerStepSize.getValue();
		boolean isShowPlotEnabled = checkBoxShowPlot.isSelected();
		boolean isGenerateColormapsEnabled = checkGenerateColormaps.isSelected();

		// Set tile dimensions
		// Note that dimensions are in pixels for sub-images.
		// Relative to the original image, the tile size is 2x these dimensions.
		int tileWidth = 64;
		int tileHeight = 64;

		BatchProcess.batchMeanTiledFRC(tileWidth, tileHeight, pixelSize, stepSize, isShowPlotEnabled, isGenerateColormapsEnabled);

		frame.dispose();
	}

	private void cancelButtonClicked()
	{
		frame.dispose();
	}

	private Double getCurrentImagePixelSize()
	{
		ImagePlus image = WindowManager.getCurrentImage();
		Calibration cal = image.getCalibration();
		double pixelWidth = cal.pixelWidth;
		double pixelHeight = cal.pixelHeight;

		if( pixelWidth != pixelHeight )
		{
			IJ.error("The pixel width of the image is different from the pixel height. This is not supported currently.");
		}

		return pixelWidth;
	}
}