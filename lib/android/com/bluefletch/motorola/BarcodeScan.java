package com.bluefletch.motorola;

/**
 * Simple class for holding barcode data
 */
public class BarcodeScan {
	public String LabelType;
	public String Barcode;
	public String Image;

	public BarcodeScan (String label, String code, String image){
		this.LabelType = label.trim();
		this.Barcode = code.trim();
		this.Image = image;
	}
}
