package com.andybotting.tramhunter;

import java.util.Vector;



public class NextTramCollection {
	
	Stop stop;
	//Vector<NextTram> trams = new Vector();
	private Vector<NextTram> trams = new Vector<NextTram>();

	
	public void setStop(Stop _stop) {
		stop = _stop;
	}
	
	
	public Stop getStop(){
		return stop;
	}
	
	
	 
	
	/**
	 * Returns a specific property at a certain index.
	 * 
	 * @param index
	 *			the index of the desired property
	 * @return the desired property
	 */
	public NextTram getTram(int index) {
		return trams.elementAt(index);
	}
	
	/**
	 * Returns the number of properties
	 * 
	 * @return the number of properties
	 */
	public int getTramCount() {
		return trams.size();
	}
	
	/**
	 * Adds a property (parameter) to the object. This is essentially a sub
	 * element.
	 * 
	 * @param propertyInfo
	 *			designated retainer of desired property
	 * @param value
	 *			the value of the property
	 */
	public void addTram(NextTram tram) {
		trams.addElement(tram);

	}
	
	
	

	public String toString() {
		
		StringBuffer buf = new StringBuffer();
		buf.append("Stop: ");
		buf.append(stop.toString());
		buf.append("\n");
		
		for (int i = 0; i < getTramCount(); i++) {
			buf.append( getTram(i).toString() );
		}
		return buf.toString();
	}
	
	
}
