/* 
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import core.SimScenario;
import core.World;
import java.util.Random;
import java.util.Set;

import routing.util.RoutingInfo;

import util.Tuple;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**
 * Implementation of Mixnet router
 */
public class MixnetEpidemicRouter extends ActiveRouter {
	
	/** Mixent router's setting namespace ({@value})*/ 
	public static final String MIXNET_NS = "MixnetEpidemicRouter";

	public int nrofmixes;
    public int nrofbundle;
    public boolean activeDebug;
	public double maxTime;
	public double delayTimer;
	public double stoprate;
	public double startrate;
	/** Range of host addresses that can be mixers */
	protected int[] mixHostRange = null;
	public Random rng;

	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public MixnetEpidemicRouter(Settings s) {
		super(s);
		Settings mixnetSettings = new Settings(MIXNET_NS);
		nrofmixes = mixnetSettings.getInt("nrofmixes");
		stoprate = mixnetSettings.getDouble("stoprate");
		startrate = mixnetSettings.getDouble("startrate");
		mixHostRange = mixnetSettings.getCsvInts("mixhosts", 2);
		nrofbundle = mixnetSettings.getInt("nrofbundle");
        maxTime = mixnetSettings.getDouble("maxTime");
        activeDebug = mixnetSettings.getBoolean("activedebug");
		//this.rng = new Random(getHost().toString().hashCode());
		this.rng = new Random(25);
	}

    @Override
	public MessageRouter replicate() {
		MixnetEpidemicRouter r = new MixnetEpidemicRouter(this);
		return r;
	}
    
	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected MixnetEpidemicRouter(MixnetEpidemicRouter r) {
		super(r);
		this.stoprate = r.stoprate;
		this.startrate = r.startrate;
		this.nrofmixes = r.nrofmixes;
		this.mixHostRange = r.mixHostRange;
		this.nrofbundle = r.nrofbundle;
		this.maxTime = r.maxTime;
		this.delayTimer = r.delayTimer;
        this.rng = r.rng;
        this.activeDebug = r.activeDebug;
		
		
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		
		Message msg = super.messageTransferred(id, from);
		msg.lastfwd = from.toString();
		boolean isFakeMsg = msg.toString().contains("f");
		if (msg.getTo() == getHost() && msg.mixindex < nrofmixes && !isFakeMsg) {
			//generate a response message
			msg.mixindex++;
			Message res = new Message(this.getHost(),msg.mixlist.get(msg.mixindex),
						"r"+msg.mixindex+"-"+msg.getId(), msg.getResponseSize());
			res.mixlist= new ArrayList<DTNHost>(msg.mixlist);
			res.mixindex=msg.mixindex;
			res.mixcreationtime=msg.mixcreationtime;
			this.createNewMessage(res);

		} else {
			addToMessages(msg, true);
			updateTimer();
		}

        if (activeDebug) {
            debugFunction();
        }
		

		return msg;
    }


    @Override 
	public boolean createNewMessage(Message msg) {

		if (msg.toString().contains("r")){
			// If the previous message had this host as mixnet

		 	makeRoomForNewMessage(msg.getSize());
			msg.setTtl(this.msgTtl);

		} 
		else if(msg.toString().contains("f")){
			// If the message is fake

			makeRoomForMessage(msg.getSize());
			msg.setTtl(this.msgTtl);

		}
		else{
			// If this node creates the message
			double simTime = SimClock.getTime();
			msg.mixcreationtime = simTime;
			SimScenario scen = SimScenario.getInstance();
			World world = scen.getWorld();
		
			double endTime = scen.getEndTime();
			double prop = simTime / endTime;

			// Only generate messages the first startrate% of the simulation
            if ( prop > stoprate || prop < startrate){
                return false;
			}

			// Add mixnet nodes
			int lastHost = msg.getFrom().getAddress();
			for (int mix=1; mix<=nrofmixes; mix++){
				int host = drawToAddress(mixHostRange, lastHost, msg.getTo().getAddress());
				lastHost = host;
				DTNHost mixnode = world.getNodeByAddress(host);
				msg.mixlist.add(mixnode);
			}

			// Add the actual host at the end
			msg.mixlist.add(msg.getTo());
			msg.setTo(msg.mixlist.get(msg.mixindex));
			makeRoomForNewMessage(msg.getSize());
			msg.setTtl(this.msgTtl);
		}

		addToMessages(msg, true);
		updateTimer();

        if (activeDebug) {
            debugFunction();
        }
		
		return true;
	}

	@Override
	public void update() {
        super.update();
        
        checkTimer();
		
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
        }

        if (getNrofMessages() >= nrofbundle) {
            tryOtherMessages();
        }

		if (SimClock.getIntTime() == 10000 && activeDebug) {
			debugFunction();
		}

	}
	
	/**
	 * (Canviar comentaris de PROPhET)
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {

		List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message, Connection>>(); 
		
		// For all connected hosts tries to send the messages
		for (Connection con : getConnections()) {

            Collection<Message> msgCollection = getMessageCollection();
			DTNHost other = con.getOtherNode(getHost());
			MixnetEpidemicRouter othRouter = (MixnetEpidemicRouter)other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : msgCollection){

				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				messages.add(new Tuple<Message, Connection>(m,con));
				
			}			
		}
		
		if (messages.size() == 0) {
			return null;
		}
		
		// Tries to send the messages
		Tuple<Message,Connection> sentMsg = tryMessagesForConnected(messages);

		return sentMsg;
	}

	protected void createFakeMessage() {
		int to = drawToAddress(mixHostRange, getHost().getAddress());
		SimScenario scen = SimScenario.getInstance();
		World world = scen.getWorld();
		Message fakeMsg = new Message(this.getHost(), world.getNodeByAddress(to),
					 "f-"+getHost()+":"+SimClock.getIntTime(),0);

		createNewMessage(fakeMsg);
    }
    
    protected void checkTimer() {
        if ((SimClock.getTime() - this.delayTimer) >= this.maxTime){
            createFakeMessage();
        }
    }

	/**
	 * Update the timer after a new message appears in the msglist
	 */
	protected void updateTimer() {
		if (this.getNrofMessages() < this.nrofbundle) {
			this.delayTimer = SimClock.getIntTime();
		} else {
			SimScenario scen = SimScenario.getInstance();
			this.delayTimer = scen.getEndTime();
		}
	}

	/**
	 * Draws a destination host address that is different from the "from"
	 * address.
	 * @param hostRange The range of hosts
	 * @param from the "from" address
	 * @return a destination address from the range, but different from "from"
	 */
	protected int drawToAddress(int hostRange[], int from) {
		int to;
		do {
			to = drawHostAddress(hostRange); 
		} while (from==to);
		
		return to;
	}

	/**
	 * Draws a destination host address that is different from the "from"
	 * address and from the real destination address
	 * @param hostRange The range of hosts
	 * @param from the "from" address
	 * @param realTo the original destination host
	 * @return a destination address from the range, but different from "from" and "realTo"
	 */
	protected int drawToAddress(int hostRange[], int from, int realTo) {
		int to;
		do {
			to = drawHostAddress(hostRange); 
		} while (from==to || realTo==to);
		
		return to;
	}

	/**
	 * Draws a host address from a range of addresses
	 * @param hostRange The range of hosts
	 * @return a destination address from the range
	 */
    protected int drawHostAddress(int hostRange[]) {
		if (hostRange[1] == hostRange[0]) {
				return hostRange[0];
		}
		return hostRange[0] + rng.nextInt(hostRange[1] - hostRange[0]);
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			
		}
    }
    
    protected void debugFunction() {
        System.out.print(SimClock.getTime()+" "+this.getHost()+":");
			for(Message m : this.getMessageCollection()){
				System.out.print(m.toString()+"->"+m.getTo()+" ");
			}
			System.out.print("\n");
			System.out.println(
				SimClock.getTime()+" "+this.getHost()+":"+this.getNrofMessages()
			);
    }
}