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
public class MixnetSnWRouter extends ActiveRouter {
	
	// SnW variables
	/** identifier for the initial number of copies setting ({@value})*/ 
	public static final String NROF_COPIES = "nrofCopies";
	/** identifier for the binary-mode setting ({@value})*/ 
	public static final String BINARY_MODE = "binaryMode";
	/** SprayAndWait router's settings name space ({@value})*/ 
	public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
	/** Message property key */
	public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." +
		"copies";
	
	protected int initialNrofCopies;
	protected boolean isBinary;
	List<Message> copiesLeft;


	// Mixnet router variables 
	public static final String MIXNET_NS = "MixnetSnWRouter";

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
	public MixnetSnWRouter(Settings s) {
		super(s);
		Settings mixnetSettings = new Settings(MIXNET_NS);
		nrofmixes = mixnetSettings.getInt("nrofmixes");
		stoprate = mixnetSettings.getDouble("stoprate");
		startrate = mixnetSettings.getDouble("startrate");
		mixHostRange = mixnetSettings.getCsvInts("mixhosts", 2);
		nrofbundle = mixnetSettings.getInt("nrofbundle");
        maxTime = mixnetSettings.getDouble("maxTime");
        activeDebug = mixnetSettings.getBoolean("activedebug");
		this.rng = new Random(25);

		initialNrofCopies = mixnetSettings.getInt(NROF_COPIES);
		isBinary = mixnetSettings.getBoolean(BINARY_MODE);
	}

    @Override
	public MessageRouter replicate() {
		MixnetSnWRouter r = new MixnetSnWRouter(this);
		return r;
	}
    
	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected MixnetSnWRouter(MixnetSnWRouter r) {
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
		this.initialNrofCopies = r.initialNrofCopies;
		this.isBinary = r.isBinary;
		this.copiesLeft = r.copiesLeft;
	}

	@Override
	public int receiveMessage(Message m, DTNHost from) {
		return super.receiveMessage(m, from);
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message msg = super.messageTransferred(id, from);
		boolean isFakeMsg = msg.toString().contains("f");
		Integer nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);
		assert nrofCopies != null : "Not a SnW message: " + msg;

		msg.lastfwd = from.toString();

		if (isBinary) {
			/* in binary S'n'W the receiving node gets ceil(n/2) copies */
			nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		}
		else {
			/* in standard S'n'W the receiving node gets only single copy */
			nrofCopies = 1;
		}
		
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

		if (msg.getTo() == getHost() && msg.mixindex < nrofmixes && !isFakeMsg) {
			//Mix message
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

		msg.addProperty(MSG_COUNT_PROPERTY, new Integer(initialNrofCopies));
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
		
		this.copiesLeft = sortByQueueMode(getMessagesWithCopiesLeft());

		if (getNrofMessages() >= this.nrofbundle) {

			if (exchangeDeliverableMessages() != null) {
				return;
			}

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

			DTNHost other = con.getOtherNode(getHost());
			MixnetSnWRouter othRouter = (MixnetSnWRouter)other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : copiesLeft){

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

	/**
	 * Creates and returns a list of messages this router is currently
	 * carrying and still has copies left to distribute (nrof copies > 1).
	 * @return A list of messages that have copies left
	 */
	protected List<Message> getMessagesWithCopiesLeft() {
		List<Message> list = new ArrayList<Message>();

		for (Message m : getMessageCollection()) {
			Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
			assert nrofCopies != null : "SnW message " + m + " didn't have " + 
				"nrof copies property!";
			if (nrofCopies > 1) {
				list.add(m);
			}
		}
		
		return list;
	}
	
	/**
	 * Called just before a transfer is finalized (by 
	 * {@link ActiveRouter#update()}).
	 * Reduces the number of copies we have left for a message. 
	 * In binary Spray and Wait, sending host is left with floor(n/2) copies,
	 * but in standard mode, nrof copies left is reduced by one. 
	 */
	@Override
	protected void transferDone(Connection con) {
		Integer nrofCopies;
		String msgId = con.getMessage().getId();
		/* get this router's copy of the message */
		Message msg = getMessage(msgId);

		if (msg == null) { // message has been dropped from the buffer after..
			return; // ..start of transfer -> no need to reduce amount of copies
		}
		
		/* reduce the amount of copies left */
		nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);
		if (isBinary) { 
			nrofCopies /= 2;
		}
		else {
			nrofCopies--;
		}
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
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