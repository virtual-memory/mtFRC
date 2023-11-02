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

		// Analysis type

		JLabel labelAnalysisType = new JLabel("Analysis Type:");
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.WEST;
		panel.add(labelAnalysisType, c);

		// Create a button group for radio buttons
		ButtonGroup buttonGroupAnalysisType = new ButtonGroup();

		// Create the first radio button
		JRadioButton radioButtonSingleImage = new JRadioButton("Single Image");
		radioButtonSingleImage.setSelected(true);
		c.gridx = 1;
		panel.add(radioButtonSingleImage, c);

		// Create the second radio button
		JRadioButton radioButtonTwoImage = new JRadioButton("Two Image");
		c.gridx = 2;
		panel.add(radioButtonTwoImage, c);

		// Add radio buttons to the button group
		buttonGroupAnalysisType.add(radioButtonSingleImage);
		buttonGroupAnalysisType.add(radioButtonTwoImage);

		// Analysis area

		JLabel labelAnalysisArea = new JLabel("Analysis Area:");
		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.WEST;
		panel.add(labelAnalysisArea, c);

		// Create a button group for radio buttons
		ButtonGroup buttonGroupAnalysisArea = new ButtonGroup();

		// Create the first radio button
		JRadioButton radioButtonRois = new JRadioButton("ROIs");
		radioButtonRois.setSelected(true);
		c.gridx = 1;
		panel.add(radioButtonRois, c);

		// Create the second radio button
		JRadioButton radioButtonWholeImage = new JRadioButton("Whole Image");
		c.gridx = 2;
		panel.add(radioButtonWholeImage, c);

		// Add radio buttons to the button group
		buttonGroupAnalysisArea.add(radioButtonRois);
		buttonGroupAnalysisArea.add(radioButtonWholeImage);

		JLabel labelPixelSize = new JLabel("Pixel Size (μm/pixel):");
		c.gridx = 0;
		c.gridy = 2;
		panel.add(labelPixelSize, c);

		double pixelSize = getCurrentImagePixelSize();

		SpinnerNumberModel modelPixelSize = new SpinnerNumberModel(pixelSize, // Initial value
				0, // Min
				Double.MAX_VALUE, // Max
				1); // Step
		JSpinner spinnerPixelSize = new JSpinner(modelPixelSize);
		Dimension d = spinnerPixelSize.getPreferredSize();
		d.width = 100;
		spinnerPixelSize.setPreferredSize(d);
		c.gridx = 1;
		panel.add(spinnerPixelSize, c);

		double stepSize = getCurrentImageZStepSize();

		JLabel labelStepSize = new JLabel("Z Step Size:");
		c.gridx = 0;
		c.gridy = 3;
		panel.add(labelStepSize, c);

		SpinnerNumberModel modelStepSize = new SpinnerNumberModel(stepSize, // Initial value
				-Double.MAX_VALUE, // Min
				Double.MAX_VALUE, // Max
				1); // Step
		JSpinner spinnerStepSize = new JSpinner(modelStepSize);
		Dimension d2 = spinnerStepSize.getPreferredSize();
		d2.width = 100;
		spinnerStepSize.setPreferredSize(d2);
		c.gridx = 1;
		panel.add(spinnerStepSize, c);

		JLabel labelTileSize = new JLabel("Tile Size:");
		c.gridx = 0;
		c.gridy = 4;
		panel.add(labelTileSize, c);

		Integer[] tileSizeOptions = new Integer[]{32, 64, 128, 256, 512, 1024};
		JComboBox<Integer> comboBoxTileOptions = new JComboBox<>(tileSizeOptions);
		comboBoxTileOptions.setPreferredSize(new Dimension(100, 25));
		comboBoxTileOptions.setSelectedItem(64);
		c.gridx = 1;
		panel.add(comboBoxTileOptions, c);

		JLabel labeltileSizeInfo = new JLabel("Size in pixels relative to sub-images.");
		Color darkBlue = new Color(0, 0, 140);
		labeltileSizeInfo.setForeground(darkBlue);
		c.gridx = 2;
		panel.add(labeltileSizeInfo, c);

		JCheckBox checkBoxShowPlot = new JCheckBox("Show plot");
		checkBoxShowPlot.setSelected(true);
		c.gridx = 0;
		c.gridy = 5;
		c.gridheight = 1;
		panel.add(checkBoxShowPlot, c);

		JCheckBox checkBoxGenerateColormaps = new JCheckBox("Generate colormaps");
		checkBoxGenerateColormaps.setSelected(true);
		c.gridy = 6;
		panel.add(checkBoxGenerateColormaps, c);

		JLabel labelColormapMin = new JLabel("Colormap min (μm):");
		c.gridx = 0;
		c.gridy = 7;
		panel.add(labelColormapMin, c);

		SpinnerNumberModel modelColormapMin = new SpinnerNumberModel(0, // Initial value
				0, // Min
				Double.MAX_VALUE, // Max
				1); // Step
		JSpinner spinnerColormapMin = new JSpinner(modelColormapMin);
		d = spinnerColormapMin.getPreferredSize();
		d.width = 100;
		spinnerColormapMin.setPreferredSize(d);
		c.gridx = 1;
		panel.add(spinnerColormapMin, c);

		JLabel labelColormapMax = new JLabel("Colormap max (μm):");
		c.gridx = 0;
		c.gridy = 8;
		panel.add(labelColormapMax, c);

		SpinnerNumberModel modelColormapMax = new SpinnerNumberModel(7, // Initial value
				0, // Min
				Double.MAX_VALUE, // Max
				1); // Step
		JSpinner spinnerColormapMax = new JSpinner(modelColormapMax);
		d = spinnerColormapMax.getPreferredSize();
		d.width = 100;
		spinnerColormapMax.setPreferredSize(d);
		c.gridx = 1;
		panel.add(spinnerColormapMax, c);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okButtonClicked(radioButtonSingleImage, radioButtonTwoImage, radioButtonRois, radioButtonWholeImage, spinnerPixelSize, spinnerStepSize, comboBoxTileOptions, checkBoxShowPlot, checkBoxGenerateColormaps, spinnerColormapMin, spinnerColormapMax);
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
		c.gridy = 9;
		c.gridwidth = 2;
		panel.add(buttonPanel, c);

		// Enable/disable 'Colormap min' and 'Colormap max' inputs based on the value of the 'Generate colormaps' checkbox
		checkBoxGenerateColormaps.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				boolean enabled = e.getStateChange() == ItemEvent.SELECTED;

				// Enable/disable spinners
				spinnerColormapMin.setEnabled(enabled);
				spinnerColormapMax.setEnabled(enabled);

				// Adjust label appearance
				labelColormapMin.setForeground(enabled ? Color.BLACK : Color.GRAY);
				labelColormapMax.setForeground(enabled ? Color.BLACK : Color.GRAY);
			}
		});

		// Show/hide the 'Tile Size' information based on the analysis type
		radioButtonTwoImage.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				boolean isTwoImageAnalysisSelected = e.getStateChange() == ItemEvent.SELECTED;

				if( isTwoImageAnalysisSelected )
				{
					labeltileSizeInfo.setForeground(new Color(0, 0, 0, 0));
				}
				else
				{
					labeltileSizeInfo.setForeground(darkBlue);
				}
			}
		});

		frame.setPreferredSize(new Dimension(650, 350));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private void okButtonClicked(JRadioButton radioButtonSingleImage, JRadioButton radioButtonTwoImage, JRadioButton radioButtonRois, JRadioButton radioButtonWholeImage, JSpinner spinnerPixelSize, JSpinner spinnerStepSize, JComboBox<Integer> comboBoxTileOptions, JCheckBox checkBoxShowPlot, JCheckBox checkGenerateColormaps, JSpinner spinnerColormapMin, JSpinner spinnerColormapMax)
	{
		boolean useSingleImageAnalysis = radioButtonSingleImage.isSelected();
		if( !useSingleImageAnalysis )
		{
			// Check that current image contains at least two channels

			ImagePlus imagePlus = WindowManager.getCurrentImage();
			int numChannels = imagePlus.getNChannels();

			if( numChannels < 2 )
			{

				IJ.error("To use Two Image analysis, add the second stack as a new channel.");
				return;
			}
		}

		boolean useRois = radioButtonRois.isSelected();
		if( useRois )
		{
			// Return if ROI Manager not open
			RoiManager manager = RoiManager.getInstance();
			if (manager == null)
			{
				IJ.error("To run mtFRC on ROIs, please first open the ROI manager and add at least 1 ROI.");
				return;
			}

			// Return if no ROIs added
			if( manager.getRoisAsArray().length < 1 )
			{
				IJ.error("At least 1 ROI is required. Please add an ROI to the ROI manager.");
				return;
			}
		}

		double pixelSize = (double) spinnerPixelSize.getValue();
		double stepSize = (double) spinnerStepSize.getValue();
		boolean isShowPlotEnabled = checkBoxShowPlot.isSelected();
		boolean isGenerateColormapsEnabled = checkGenerateColormaps.isSelected();
		double colormapUmMin = (double) spinnerColormapMin.getValue();
		double colormapUmMax = (double) spinnerColormapMax.getValue();

		// Set tile dimensions
		// For Single Image analysis, note that 'tileWidth' and 'tileHeight' use dimensions in pixels for sub-images.
		// Relative to the original image, the tile size is 2x these dimensions.
		int tileWidth = (int)comboBoxTileOptions.getSelectedItem();
		int tileHeight = tileWidth;

		BatchProcess.batchMeanTiledFRC(useSingleImageAnalysis, useRois, tileWidth, tileHeight, pixelSize, stepSize, isShowPlotEnabled, isGenerateColormapsEnabled, colormapUmMin, colormapUmMax);

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

	private Double getCurrentImageZStepSize()
	{
		ImagePlus image = WindowManager.getCurrentImage();
		Calibration cal = image.getCalibration();

		return cal.pixelDepth;
	}
}