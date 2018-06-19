package com.bluefletch.motorola;

/**
 * Simple class for holding barcode data
 */
public class BarcodeScan {
	public String LabelType;
	public String Barcode;
	public BarcodeScan (String label, String code){
		this.LabelType = label.trim();
		this.Barcode = code.trim();
	}
}
