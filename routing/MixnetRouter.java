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
import java.util.List;
import java.util.Map;
import core.SimScenario;
import core.World;
import java.util.Random;

import routing.util.RoutingInfo;

import util.Tuple;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**
 * Implementation of PRoPHET router as described in 
 * <I>Probabilistic routing in intermittently connected networks</I> by
 * Anders Lindgren et al.
 */
public class MixnetRouter extends ActiveRouter {
	
	/** Prophet router's setting namespace ({@value})*/ 
	public static final String PROPHET_NS = "MixnetRouter";

	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of 
	 * delivery predictions. Should be tweaked for the scenario.*/
	public int nrofmixes; 
	public int broadcast;

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
	public MixnetRouter(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(PROPHET_NS);
		nrofmixes = prophetSettings.getInt("nrofmixes");
		stoprate = prophetSettings.getDouble("stoprate");
		startrate = prophetSettings.getDouble("startrate");
		broadcast = prophetSettings.getInt("broadcast");
		mixHostRange = prophetSettings.getCsvInts("mixhosts", 2);
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
		this.rng = r.rng;
		
		
	}


	@Override
    protected void transferDone(Connection con) {
            /* don't leave a copy for the sender */
		if (broadcast == 0){
            this.deleteMessage(con.getMessage().getId(), false);
		}
    }

	/**
	 * Modificat per mixnets. 
	 * Crea el missatge amb el super i afegeix les noves opcions.
	 */
	@Override
	public Message messageTransferred(String id, DTNHost from) {
	
		Message msg = super.messageTransferred(id, from);
		msg.lastfwd = from.toString();
		if (msg.getTo() == getHost() && msg.mixindex < nrofmixes) {
			// generate a response message
			msg.mixindex++;
			Message res = new Message(this.getHost(),msg.mixlist.get(msg.mixindex),
						"r"+msg.mixindex+"-"+msg.getId(), msg.getResponseSize());
			res.mixlist= new ArrayList<DTNHost>(msg.mixlist);
			res.mixindex=msg.mixindex;
			res.mixcreationtime=msg.mixcreationtime;
			this.createNewMessage(res);
			//this.getMessage(RESPONSE_PREFIX+m.getId()).setRequest(m);
		}

	return msg;
    }


    @Override 
	public boolean createNewMessage(Message msg) {

		if (msg.toString().contains("r")){
			// Si el missatge es una resposta, es a dir, aquest es el seu node desti.
		 	makeRoomForNewMessage(msg.getSize());
			msg.setTtl(this.msgTtl);
			addToMessages(msg, true);
			return true;
		}
		else{
			// Si aquest node crea un missatge (?)
			double simTime = SimClock.getTime();
			msg.mixcreationtime = simTime;
			SimScenario scen = SimScenario.getInstance();
			World world = scen.getWorld();
		
			double endTime = scen.getEndTime();
			double prop = simTime / endTime;
			//Not finished simulation are it is not response
            if ( prop > stoprate || prop < startrate){
                return false;
			}
			// Agafem nrofmixes nodes mixnet i els posem a la llista a recorrer
			for (int mix=1; mix<=nrofmixes;mix++){
		
				int host = drawHostAddress(mixHostRange);
				DTNHost mixnode = world.getNodeByAddress(host);
				msg.mixlist.add(mixnode);
				//System.out.println("Added mix " + mixnode + " to message " + msg);
			
			}

			// Agefeix el desti real al final de la llista
			// [mix1, mix2, mixN, desti]
			// i posa com a desti actual el mix1 (mixindex = 0)
			msg.mixlist.add(msg.getTo());
			msg.setTo(msg.mixlist.get(msg.mixindex));
			makeRoomForNewMessage(msg.getSize());

			msg.setTtl(this.msgTtl);
			addToMessages(msg, true);
			return true;
	    }
    }

	/**
	 *  Utilitzat per escollir nodes mixnet pel "cami"
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
	
	/**
	 * Es crida cada 1min de la simulació
	 */
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		//System.out.println("AH");	
		tryOtherMessages();	
		//System.out.println("BH");
	
	}
	
	/**
	 * (Canviar comentaris de PROPhET)
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages = 
			new ArrayList<Tuple<Message, Connection>>(); 
	
		Collection<Message> msgCollection = getMessageCollection();
		
		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			MixnetRouter othRouter = (MixnetRouter)other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}
			
			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				if (true) {
					// the other node has higher probability of delivery
					messages.add(new Tuple<Message, Connection>(m,con));
				}
			}			
		}
		
		if (messages.size() == 0) {
			return null;
		}
		
		// sort the message-connection tuples
		//Collections.sort(messages, new TupleComparator());
		return tryMessagesForConnected(messages);	// try to send messages
	}
	

	

}
