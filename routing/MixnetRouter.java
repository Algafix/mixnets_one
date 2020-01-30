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

			if(msg.getTo() == this.getHost()) {
				messageTransferred(msg.getId(), this.getHost());
			}

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

			//Només genera missatges en el primer 20% de la simulacio
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
	 *  Utilitzat per escollir nodes mixnet pel "cami"
	 */
    protected int drawHostAddress(int hostRange[]) {
		if (hostRange[1] == hostRange[0]) {
				return hostRange[0];
		}
		return hostRange[0] + rng.nextInt(hostRange[1] - hostRange[0]);
	}

	/**
	 * Draws a destination host address that is different from the "from"
	 * address
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
	 * ??????
	 */
	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			
		}
	}
	
	/**
	 * Es crida cada 1s de la simulació
	 */
	@Override
	public void update() {
		super.update();
		
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}

		//exchangeDeliverableMessages();

		tryOtherMessages();

	}
	
	/**
	 * (Canviar comentaris de PROPhET)
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {


		// Missatges a enviar a la connexio
		List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message, Connection>>(); 
		
		// Missatges que té el router
		Collection<Message> msgCollection = getMessageCollection();

		// Fa una copia temporal de la variable que conté el número de missatges per node
		Map<DTNHost,Integer> tempNodeCount = new HashMap<>(nodeCount);
		
		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			MixnetRouter othRouter = (MixnetRouter)other.getRouter();
			
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			if (nodeCount.getOrDefault(other,0) < nrofbundle) {
				continue; // no transmet al host si no tenim prous missatges
			}
			
			for (Message m : msgCollection) {

				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}

				// Només transfereix si l'altre es el destinatari final
				if (m.getTo() == other) {

					messages.add(new Tuple<Message, Connection>(m,con));

					int tCountValue = tempNodeCount.get(m.getTo());
					tempNodeCount.put(m.getTo(), tCountValue < 1 ? 0 : tCountValue-1);

					System.out.println(SimClock.getTime()+" "+this.getHost()+":"+tempNodeCount);
				}
			}			
		}
		
		if (messages.size() == 0) {
			return null;
		}
		
		// Actualitza la variable de recompte
		nodeCount = tempNodeCount;
		
		// Transfereix tots els missatges a la connexió indicada
		return tryMessagesForConnected(messages);
	}	

}
