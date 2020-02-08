/* 
 * Created by Carlos Borrego cborrego@ifae.es
 * 
 */
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance per application. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class MessageStatsReportPerApplication extends Report implements MessageListener {
	private Map<String, Double> creationTimes;
	private Hashtable <String,ArrayList<Double>> latencieslist;
	private Hashtable <String,ArrayList<Integer>> hopCountslist;
	private Hashtable <String,ArrayList<Double>> msgBufferTimelist;
	private Hashtable <String,ArrayList<Double>> rttlist; // round trip times
	
	
	private Hashtable <String,Integer> nrofDroppedlist;
	private Hashtable <String,Integer> nrofRemovedlist;
	private Hashtable <String,Integer> nrofStartedlist;
	private Hashtable <String,Integer> nrofAbortedlist;
	private Hashtable <String,Integer> nrofRelayedlist;
        private Hashtable <String,Integer> nrofRelayedPerNodelist;
	private Hashtable <String,Integer> nrofCreatedlist;
	private Hashtable <String,Integer> nrofResponseReqCreatedlist;
	private Hashtable <String,Integer> nrofResponseDeliveredlist;
	private Hashtable <String,Integer> nrofDeliveredlist;
        private Hashtable <String,String> deployList;

	
	/**
	 * Constructor.
	 */
	public MessageStatsReportPerApplication() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.creationTimes = new HashMap<String, Double>();
		this.latencieslist = new Hashtable <String,ArrayList<Double>>();
		this.msgBufferTimelist= new Hashtable <String,ArrayList<Double>>();
		this.hopCountslist = new Hashtable <String,ArrayList<Integer>>();
		this.rttlist = new Hashtable <String,ArrayList<Double>>();
		
		this.nrofDroppedlist = new Hashtable <String,Integer>();
                this.nrofRemovedlist = new Hashtable <String,Integer>();
                this.nrofStartedlist = new Hashtable <String,Integer>();
                this.nrofAbortedlist = new Hashtable <String,Integer>();
                this.nrofRelayedlist = new Hashtable <String,Integer>();
                this.deployList = new Hashtable <String,String>();
                this.nrofRelayedPerNodelist = new Hashtable <String,Integer>();
                this.nrofCreatedlist = new Hashtable <String,Integer>();
                this.nrofResponseReqCreatedlist = new Hashtable <String,Integer>();
                this.nrofResponseDeliveredlist = new Hashtable <String,Integer>();
                this.nrofDeliveredlist = new Hashtable <String,Integer>();

	}

	public String getApplication(Message m)
	{
	//return m.getAppID();
	//return Character.toString(m.getId().charAt(0));
	return m.toString().replaceAll("[0-9]+", "");
	//return m.toString().replaceAll("[^a-z]+", "");
	}
        public String getNode(DTNHost host)
        {
        //return Character.toString(m.getId().charAt(0));
        
        return host.toString().replaceAll("[^a-z]+", "");
        }	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		String app = getApplication(m);
		if (isWarmupID(m.getId())) {
			return;
		}
	        	
		if (dropped) {
			if (nrofDroppedlist.containsKey(app)){
				int i = this.nrofDroppedlist.get(app) + 1;
				this.nrofDroppedlist.put(app,i);
 			}
			else
 			{
			nrofDroppedlist.put(app,1);
 			}

		}
		else {
		if (nrofRemovedlist.containsKey(app)){
                                int i = this.nrofRemovedlist.get(app) + 1;
                                this.nrofRemovedlist.put(app,i);
                }
                else
                {
                        nrofRemovedlist.put(app,1);
                }

		}
		if (this.msgBufferTimelist.containsKey(app)){	
			ArrayList<Double> tmp = this.msgBufferTimelist.get(app);
			tmp.add(getSimTime() - m.getReceiveTime());		
		}
		else
		{
			ArrayList<Double> tmp = new ArrayList<Double>();
			tmp.add(getSimTime() - m.getReceiveTime());
			this.msgBufferTimelist.put(app,tmp);
		}
	}

	
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
	 	String app = getApplication(m);
	
		if (nrofAbortedlist.containsKey(app)){
                                int i = this.nrofAbortedlist.get(app) + 1;
                                this.nrofAbortedlist.put(app,i);
                }
                else
                {
                        nrofAbortedlist.put(app,1);
                }
	
	}

	
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean finalTarget) {
		if (isWarmupID(m.getId())) {
			return;
		}
		String app = getApplication(m);

                if (nrofRelayedlist.containsKey(app)){
                                int i = this.nrofRelayedlist.get(app) + 1;
                                this.nrofRelayedlist.put(app,i);
                }
                else
                {
                        nrofRelayedlist.put(app,1);
                }
		String nodetype = getNode(from);
                if (nrofRelayedPerNodelist.containsKey(nodetype)){
                                int i = this.nrofRelayedPerNodelist.get(nodetype) + 1;
                                this.nrofRelayedPerNodelist.put(nodetype,i);
                }
                else
                {
                        nrofRelayedPerNodelist.put(nodetype,1);
                }

		//if (deployList.containsKey(to.toString())){
                //                String i = this.deployList.get(to.toString());
                 //               this.nrofRelayedPerNodelist.put(nodetype,i);
                //}
                //else
                //{
                 //       nrofRelayedPerNodelist.put(nodetype,1);
                //}


		if (finalTarget) {
			if (this.latencieslist.containsKey(app)){
	                        ArrayList<Double> tmp = this.latencieslist.get(app);
        	                tmp.add(getSimTime() - this.creationTimes.get(m.getId()) );
                	}
                	else
                	{
                       	 ArrayList<Double> tmp = new ArrayList<Double>();
                       	 tmp.add(getSimTime() - this.creationTimes.get(m.getId()));
                         this.latencieslist.put(app,tmp);
                	}
			
			
			if (nrofDeliveredlist.containsKey(app)){
                                int i = this.nrofDeliveredlist.get(app) + 1;
                                this.nrofDeliveredlist.put(app,i);
	                }
        	        else
                	{
                        	nrofDeliveredlist.put(app,1);
                	}	

			
			if (this.hopCountslist.containsKey(app)){
                                ArrayList<Integer> tmp = this.hopCountslist.get(app);
                                tmp.add(m.getHops().size() - 1);
                        }
                        else
                        {
                         ArrayList<Integer> tmp = new ArrayList<Integer>();
                         tmp.add(m.getHops().size() - 1);
                         this.hopCountslist.put(app,tmp);
                        }

			
			if (m.isResponse()) {
				if (this.rttlist.containsKey(app)){
	                                ArrayList<Double> tmp = this.rttlist.get(app);
        	                        tmp.add(getSimTime() -     m.getRequest().getCreationTime());
                	        }
                        	else
                        	{	
                         		ArrayList<Double> tmp = new ArrayList<Double>();
                         		tmp.add(getSimTime() -     m.getRequest().getCreationTime());
                         		this.rttlist.put(app,tmp);
                        	}
				if (nrofResponseDeliveredlist.containsKey(app)){
	                                int i = this.nrofResponseDeliveredlist.get(app) + 1;
        	                        this.nrofResponseDeliveredlist.put(app,i);
                	        }
                        	else
                        	{
                                	nrofResponseDeliveredlist.put(app,1);
                        	}	

			}
		}
	}


	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		String app = getApplication(m);

		this.creationTimes.put(m.getId(), getSimTime());
	        
	
		if (nrofCreatedlist.containsKey(app)){
                        int i = this.nrofCreatedlist.get(app) + 1;
                        this.nrofCreatedlist.put(app,i);
                 }
                 else
                 {
                     this.nrofCreatedlist.put(app,1);
		     this.nrofDroppedlist.put(app,0);
               	     this.nrofRemovedlist.put(app,0);
                     this.nrofStartedlist.put(app,0);
                     this.nrofAbortedlist.put(app,0);
                     this.nrofRelayedlist.put(app,0);
                     this.nrofResponseReqCreatedlist.put(app,0);
                     this.nrofResponseDeliveredlist.put(app,0);
                     this.nrofDeliveredlist.put(app,0);


                 }

		if (m.getResponseSize() > 0) {
			if (nrofResponseReqCreatedlist.containsKey(app)){
                        int i = this.nrofResponseReqCreatedlist.get(app) + 1;
                        this.nrofResponseReqCreatedlist.put(app,i);
                 	}
                 	else
                 	{
                 	    	nrofResponseReqCreatedlist.put(app,1);
                	}

		}
	}
	
	
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
		String app = getApplication(m);

		if (nrofStartedlist.containsKey(app)){
	                int i = this.nrofStartedlist.get(app) + 1;
                        this.nrofStartedlist.put(app,i);
                }
                else
                {
                        nrofStartedlist.put(app,1);
                }

	}
	
	@Override
	public void done() {
		write("Message stats for scenario " + getScenarioName() +
                                "\nsim_time: " + format(getSimTime()));


		Enumeration nodetypes = nrofRelayedPerNodelist.keys();
		write("Message stats for nodetypes "+nodetypes);

		while(nodetypes.hasMoreElements()) {
			String nodetype = (String) nodetypes.nextElement();
                         write("Message stats for nodetype " + nodetype);
                         write("===================================");
			String statsText = "relayed: " + this.nrofRelayedPerNodelist.get(nodetype);
	                write(statsText);
        	        }
	
		Enumeration apps = nrofStartedlist.keys();
		while(apps.hasMoreElements()) {
			 String app = (String) apps.nextElement();
		write("Message stats for application " + app);
		write("===================================");

		double deliveryProb = 0; // delivery probability
		double responseProb = 0; // request-response success probability
		double overHead = Double.NaN;	// overhead ratio
		
		if (this.nrofCreatedlist.get(app) > 0 && this.nrofDeliveredlist.containsKey(app)) {
			deliveryProb = (1.0 * this.nrofDeliveredlist.get(app)) 
			/ this.nrofCreatedlist.get(app);
		}
		if (this.nrofDeliveredlist.get(app) > 0 && this.nrofRelayedlist.containsKey(app)) {
			overHead = (1.0 * (this.nrofRelayedlist.get(app) - this.nrofDeliveredlist.get(app))) /
				this.nrofDeliveredlist.get(app);
		}
		if (this.nrofResponseReqCreatedlist.get(app) > 0) {
			responseProb = (1.0* this.nrofResponseDeliveredlist.get(app)) / 
				this.nrofResponseReqCreatedlist.get(app);
		}
		String statsText = "created: " + this.nrofCreatedlist.get(app);
		write(statsText);
		statsText = "started: " + this.nrofStartedlist.get(app);
		write(statsText);
		statsText = "relayed: " + this.nrofRelayedlist.get(app); 
                write(statsText);
		statsText = "aborted: " + this.nrofAbortedlist.get(app);
	        write(statsText);
		statsText = "dropped: " + this.nrofDroppedlist.get(app);	
		write(statsText);
		statsText = "removed: " + this.nrofRemovedlist.get(app);
		write(statsText);
		statsText = "delivered: " + this.nrofDeliveredlist.get(app);
		write(statsText);
		statsText = "delivery_prob: " + format(deliveryProb);
		write(statsText);
		statsText = "response_prob: " + format(responseProb);
		write(statsText);
		statsText = "noverhead_ratio: " + format(overHead);
		write(statsText);

		if (this.latencieslist.containsKey(app)){
		 statsText = "latency_avg: " + getAverage(this.latencieslist.get(app)) + "\nlatency_med: " + getMedian(this.latencieslist.get(app));
		}
		else
		{
		 statsText = "latency_avg: NaN\nlatency_med: NaN";
		}
		write(statsText);
		if (this.hopCountslist.containsKey(app)){
		 statsText = "hopcount_avg: " + getIntAverage(this.hopCountslist.get(app)) + "\nhopcount_med_med: " + getIntMedian(this.hopCountslist.get(app));
		}
		else
		{
		statsText = "hopcount_avg: NaN\nhopcount_med: NaN";
		}
		write(statsText);
		if (this.msgBufferTimelist.containsKey(app)){
		 statsText = "buffertime_avg: " + getAverage(this.msgBufferTimelist.get(app)) + "\nbuffertime_med: " + getMedian(this.msgBufferTimelist.get(app));
		}
		else
		{
		statsText = "buffertime_avg: NaN\nbuffertime_med: NaN";
		}
		write(statsText);
		if (this.rttlist.containsKey(app)){
		statsText = "rtt_avg: " + getAverage(this.rttlist.get(app)) + "\nrtt_med: " + getMedian(this.rttlist.get(app));
		}
		else
		{
		statsText = "rtt_avg: NaN\nrtt_med: NaN";
		}
		write(statsText);
			
		
		}
		super.done();

	}
         public void messageManycastTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery){}
	
}
