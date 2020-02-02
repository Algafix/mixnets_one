/* 
 * Copyright 2010 Aalto University, ComNet
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
public class MixnetRouter extends ActiveRouter {
	
	/** Mixent router's setting namespace ({@value})*/ 
	public static final String MIXNET_NS = "MixnetRouter";

	public int nrofmixes; 
	public int broadcast;
	public int nrofbundle;
	public double stoprate;
	public double startrate;
	/** Range of host addresses that can be mixers */
	protected int[] mixHostRange = null;
	public Random rng;

	/**
	 * Map that contains the number of messages for each node saved in this router.
	 */
	protected Map<DTNHost,Integer> nodeCount = new HashMap<>();

	/**
	 * Messages that can be delivered according to the mixnet especifications
	 */
	protected Set<Message> pendingMessages = new HashSet<>();

	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public MixnetRouter(Settings s) {
		super(s);
		Settings mixnetSettings = new Settings(MIXNET_NS);
		nrofmixes = mixnetSettings.getInt("nrofmixes");
		stoprate = mixnetSettings.getDouble("stoprate");
		startrate = mixnetSettings.getDouble("startrate");
		broadcast = mixnetSettings.getInt("broadcast");
		mixHostRange = mixnetSettings.getCsvInts("mixhosts", 2);
		nrofbundle = mixnetSettings.getInt("nrofbundle");
		//this.rng = new Random(getHost().toString().hashCode());
		this.rng = new Random(25);
	}

    @Override
	public MessageRouter replicate() {
		MixnetRouter r = new MixnetRouter(this);
		return r;
	}
    
	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected MixnetRouter(MixnetRouter r) {
		super(r);
		this.stoprate = r.stoprate;
		this.startrate = r.startrate;
		this.nrofmixes = r.nrofmixes;
		this.broadcast = r.broadcast;
		this.mixHostRange = r.mixHostRange;
		this.nrofbundle = r.nrofbundle;
		this.rng = r.rng;
		
		
	}

	/**
	 * Method is called just before a transfer is finalized 
	 * at {@link #update()}.
	 * @param con The connection whose transfer was finalized
	 */
	@Override
    protected void transferDone(Connection con) {
		// Don't leave a copy on the sender
		if (broadcast == 0){
			Message m = con.getMessage();
			if (m.mixindex < nrofmixes) {
				// It's not the final node, don't inform
				this.removeFromMessages(m.getId());
			} else {
				// It's the final node, inform listeners
				this.deleteMessage(m.getId(), false);
			}
		}
    }


	@Override
	public Message messageTransferred(String id, DTNHost from) {
		
		Message msg = super.messageTransferred(id, from);
		msg.lastfwd = from.toString();
		if (msg.getTo() == getHost() && msg.mixindex < nrofmixes) {
			//generate a response message
			msg.mixindex++;
			Message res = new Message(this.getHost(),msg.mixlist.get(msg.mixindex),
						"r"+msg.mixindex+"-"+msg.getId(), msg.getResponseSize());
			res.mixlist= new ArrayList<DTNHost>(msg.mixlist);
			res.mixindex=msg.mixindex;
			res.mixcreationtime=msg.mixcreationtime;
			this.createNewMessage(res);
		}

	return msg;
    }


    @Override 
	public boolean createNewMessage(Message msg) {

		if (msg.toString().contains("r")){
			// Si el missatge es una resposta, ja ha estat mixejat abans

		 	makeRoomForNewMessage(msg.getSize());
			msg.setTtl(this.msgTtl);
			addToMessages(msg, true);
			addToHostCount(msg);

			
			System.out.print(SimClock.getTime()+" "+this.getHost()+":");
			for(Message m : this.getMessageCollection()){
				System.out.print(m.toString()+"->"+m.getTo()+" ");
			}
			System.out.print("\n");
			System.out.println(
				SimClock.getTime()+" "+this.getHost()+":"+this.nodeCount
			);
			

			return true;
		}
		else{
			// Si aquest node crea un missatge
			double simTime = SimClock.getTime();
			msg.mixcreationtime = simTime;
			SimScenario scen = SimScenario.getInstance();
			World world = scen.getWorld();
		
			double endTime = scen.getEndTime();
			double prop = simTime / endTime;

			//Només genera missatges en el primer startrate% de la simulacio
            if ( prop > stoprate || prop < startrate){
                return false;
			}

			// Agafem nrofmixes nodes mixnet i els posem a la llista a recorrer
			int lastHost = msg.getFrom().getAddress();
			for (int mix=1; mix<=nrofmixes; mix++){
				int host = drawToAddress(mixHostRange, lastHost, msg.getTo().getAddress());
				lastHost = host;
				DTNHost mixnode = world.getNodeByAddress(host);
				msg.mixlist.add(mixnode);
			}

			// Afegeix el desti real al final de la llista i posa com a desti el mix1
			msg.mixlist.add(msg.getTo());
			msg.setTo(msg.mixlist.get(msg.mixindex));
			makeRoomForNewMessage(msg.getSize());

			msg.setTtl(this.msgTtl);
			addToMessages(msg, true);
			addToHostCount(msg);

			
			System.out.print(SimClock.getTime()+" "+this.getHost()+":");
			for(Message m : this.getMessageCollection()){
				System.out.print(m.toString()+"->"+m.getTo()+" ");
			}
			System.out.print("\n");
			System.out.println(
				SimClock.getTime()+" "+this.getHost()+":"+this.nodeCount
			);
			
			return true;
	    }
	}


	/**
	 * Adds the "to" (destination) of the message to the count of hosts
	 * @param msg Message wich destination will be added
	 */
	protected void addToHostCount(Message msg) {
		DTNHost nextHost = msg.getTo();
		nodeCount.put(
			nextHost, nodeCount.getOrDefault(nextHost, 0) + 1);
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

	/**
	 * ??????
	 */
	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			
		}
	}
	

	@Override
	public void update() {
		super.update();
		
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}

		updatePendingMessages();

		tryOtherMessages();

		if (SimClock.getIntTime() == 10000){
			System.out.print(SimClock.getTime()+" "+this.getHost()+":");
			for(Message m : this.getMessageCollection()){
				System.out.print(m.toString()+"->"+m.getTo()+" ");
			}
			System.out.print("\n");
			System.out.println(
				SimClock.getTime()+" "+this.getHost()+":"+this.nodeCount
			);
		}

	}
	
	/**
	 * (Canviar comentaris de PROPhET)
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {


		// Missatges a enviar
		List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message, Connection>>(); 
		
		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		for (Connection con : getConnections()) {

			DTNHost other = con.getOtherNode(getHost());
			MixnetRouter othRouter = (MixnetRouter)other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : pendingMessages){

				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}

				// Només transfereix si l'altre es el destinatari final
				if (m.getTo() == other) {
					messages.add(new Tuple<Message, Connection>(m,con));
				}
			}			
		}
		
		if (messages.size() == 0) {
			return null;
		}
		
		// Intenta transferir els missatges a les connexions
		Tuple<Message,Connection> sentMsg = tryMessagesForConnected(messages);
		removeFromPendingMessages(sentMsg);

		return sentMsg;
	}

	public void updatePendingMessages() {
		for(Message m : this.getMessageCollection()){
			if (nodeCount.getOrDefault(m.getTo(),0) >= nrofbundle) {
				pendingMessages.add(m);
			}
		}
	}

	private void removeFromPendingMessages(Tuple<Message,Connection> sentMsg) {
		if (sentMsg != null){
			Message m = sentMsg.getKey();
			pendingMessages.remove(m);
			nodeCount.put(m.getTo(), nodeCount.getOrDefault(m.getTo(), 0) < 1 ? 0 : nodeCount.get(m.getTo())-1);
		}
	}
}