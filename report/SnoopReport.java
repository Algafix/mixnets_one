/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.ConnectionListener;
import core.Settings;
import core.SimClock;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

//http://math.nist.gov/javanumerics/jama/doc/

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class SnoopReport extends Report implements MessageListener, ConnectionListener  {
        protected HashMap<ConnectionInfo, ConnectionInfo> connections;
	private Vector<Integer> nrofContacts;
	public static final String GRANULARITY = "granularity";
        protected double granularity;
	public double gamma;
	public double secondsInTimeUnit;
	private int[] encounters;
	
	/**
	 * Constructor.
	 */
	public SnoopReport() {

	  Settings settings = getSettings();
	  if (settings.contains(GRANULARITY)) {
	    this.granularity = settings.getDouble(GRANULARITY);
	   }
	   else {
	   this.granularity = 1.0;
	   }
	  if (settings.contains("gamma")) {
            this.gamma = settings.getDouble("gamma");
           }
           else {
           this.gamma = 0.98;
           }
	  if (settings.contains("secondsInTimeUnit")) {
            this.secondsInTimeUnit = settings.getDouble("secondsInTimeUnit");
           }
           else {
           this.secondsInTimeUnit = 30;
           }


		
		init();
	}

	public void hostsConnected(DTNHost host1, DTNHost host2) {
	 addConnection(host1, host2);
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {

	    	newEvent();
		ConnectionInfo ci = removeConnection(host1, host2);
		
		if (ci == null) {
			return; /* the connection was started during the warm up period */
		}
		
		ci.connectionEnd();
		increaseTimeCount(ci.getConnectionTime());

	}

	public void updated(List<DTNHost> hosts) {
		if (encounters == null) {
			encounters = new int[hosts.size()];
		}
	}

	public int[] getEncounters() {
		return encounters;
	}

	public void setEncounters(int[] encounters) {
		this.encounters = encounters;
	}
	


	@Override
	protected void init() {
		super.init();
		this.connections = new HashMap<ConnectionInfo,ConnectionInfo>();
		this.nrofContacts = new Vector<Integer>();
	}


 
	
	protected void addConnection(DTNHost host1, DTNHost host2) {

		ConnectionInfo ci = new ConnectionInfo(host1, host2);
		assert !connections.containsKey(ci) : "Already contained "+
			" a connection of " + host1 + " and " + host2;
		connections.put(ci,ci);
	}
	
	protected ConnectionInfo removeConnection(DTNHost host1, DTNHost host2) {

		ConnectionInfo ci = new ConnectionInfo(host1, host2);
		ci = connections.remove(ci);

		//Fcentrality
		return ci;
	}
		
	/**
	 * Increases the amount of times a certain time value has been seen.
	 * @param time The time value that was seen
	 */
	protected void increaseTimeCount(double time) {
		int index = (int)(time/this.granularity);
		
		if (index >= this.nrofContacts.size()) {
			/* if biggest index so far, fill array with nulls up to 
			  index+2 to keep the last time count always zero */
			this.nrofContacts.setSize(index + 2);
		}
		
		Integer curValue = this.nrofContacts.get(index); 
		if (curValue == null) { // no value found -> put the first
			this.nrofContacts.set(index, 1); 
		}
		else { // value found -> increase the number by one
			this.nrofContacts.set(index, curValue+1);
		}
	}
	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
	}

	
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		
	}

	
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean finalTarget) {

	if (finalTarget){
	write(getScenarioName()+ " " + getSimTime() + " " + from + " " + to + " " + m + " " + m.getFrom()+ " " + m.getTo()+ " Final");
	}
	else{
	write(getScenarioName()+ " " + getSimTime() + " " + from + " " + to + " " + m + " " + m.getFrom()+ " " + m.getTo());
	}

	}


	public void newMessage(Message m) {

		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}

	}
	
	
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {

	}
	

	@Override
	public void done() {
		
		super.done();
	}


	/**
	 * Objects of this class store time information about contacts.
	 */
	protected class ConnectionInfo {
		private double startTime;
		private double endTime;
		private DTNHost h1;
		private DTNHost h2;
		
		public ConnectionInfo (DTNHost h1, DTNHost h2){
			this.h1 = h1;
			this.h2 = h2;
			this.startTime = getSimTime();
			this.endTime = -1;
		}
		
		/**
		 * Should be called when the connection ended to record the time.
		 * Otherwise {@link #getConnectionTime()} will use end time as
		 * the time of the request.
		 */
		public void connectionEnd() {
			this.endTime = getSimTime();
		}
		
		/**
		 * Returns the time that passed between creation of this info 
		 * and call to {@link #connectionEnd()}. Unless connectionEnd() is 
		 * called, the difference between start time and current sim time
		 * is returned.
		 * @return The amount of simulated seconds passed between creation of
		 * this info and calling connectionEnd()
		 */
		public double getConnectionTime() {
			if (this.endTime == -1) {
				return getSimTime() - this.startTime;
			}			
			else {
				return this.endTime - this.startTime;
			}
		}
		
		/**
		 * Returns true if the other connection info contains the same hosts. 
		 */
		public boolean equals(Object other) {
			if (!(other instanceof ConnectionInfo)) {
				return false;
			}
			
			ConnectionInfo ci = (ConnectionInfo)other;

			if ((h1 == ci.h1 && h2 == ci.h2)) {
				return true;
			}
			else if ((h1 == ci.h2 && h2 == ci.h1)) {
				// bidirectional connection the other way
				return true;
			}
			return false;
		}
		
		/**
		 * Returns the same hash for ConnectionInfos that have the
		 * same two hosts.
		 * @return Hash code
		 */
		public int hashCode() {
			String hostString;

			if (this.h1.compareTo(this.h2) < 0) {
				hostString = h1.toString() + "-" + h2.toString();
			}
			else {
				hostString = h2.toString() + "-" + h1.toString();
			}
			
			return hostString.hashCode();
		}
		
		/**
		 * Returns a string representation of the info object
		 * @return a string representation of the info object
		 */
		public String toString() {
			return this.h1 + "<->" + this.h2 + " [" + this.startTime
				+"-"+ (this.endTime >0 ? this.endTime : "n/a") + "]";
		}
	}

	
}
