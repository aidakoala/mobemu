/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu;

import java.util.*;

import mobemu.algorithms.Epidemic;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.Stats;
import mobemu.parsers.HCMM;
import mobemu.trace.Parser;

/**
 * Main class for MobEmu.
 *
 * @author Radu
 */
public class MobEmu {

	public static void main(String[] args) {

		int communities = 50;
		int travelers = 10;
		boolean showRun = true;
		long startTime = System.nanoTime();
		// parametrii relevant sunt descrisi in continuare:
		// 100 de noduri, durata de 5 ore, viteza unui nod intre 0.25 si 1 m/s,
		// dimensiunea spatiului de simulare de 200 pe 200 m,
		// nr de celule in care e impartit spatiul de simulare este de 20 pe 20 (nu e
		// obligatoriu sa fie dependent de dimensiunea spatiului),
		// raza de transmisie de 10 m (cam atat bate Bluetooth), 50 comunitati (adica 50
		// de grupuri de prieteni, daca vrei, vezi
		// int-ul definit mai sus), 10 noduri traveler (astea inseamna
		// noduri care tind sa se plimbe dintr-o comunitate in alta, deci cei care
		// cunosc mai multe persoane la concert, poti sa te joci cu
		// valorile astea ptr comunitate si travelers, si nu trebuie
		// neaparat sa depinda una de alta), viteza nodurilor traveler este de 1 m/s;
		// daca vrei sa vezi si o reprezentare vizuala a simularii, pune booleanul
		// showRun (declarat mai sus) pe true
		Parser parser = new HCMM(100, 5 * 3600, 300, 0.25f, 1f, 0.1f, 200f, 200f, 20, 20, 10.0, 0.7, communities,
				travelers, 1f, 0.8f, 0, showRun, 10, false);
		// Parser parser = new UPB(UPB.UpbTrace.UPB2012, false);
		// Parser parser = new Sigcomm();
		// Parser parser = new UPB(UPB.UpbTrace.UPB2012);
		long estimatedTime = System.nanoTime() - startTime;
		startTime = System.nanoTime();
		System.out.println("Trace generation duration: " + estimatedTime * 1e-9);

		// print some trace statistics
		double duration = (double) (parser.getTraceData().getEndTime() - parser.getTraceData().getStartTime())
				/ (Parser.MILLIS_PER_MINUTE * 60);
		System.out.println("Trace duration in hours: " + duration);
		System.out.println("Trace contacts: " + parser.getTraceData().getContactsCount());
		System.out.println("Trace contacts per hour: " + (parser.getTraceData().getContactsCount() / duration));
		System.out.println("Nodes: " + parser.getNodesNumber());

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(parser.getTraceData().getStartTime());
		System.out.println(cal.getTime());

		// initialize nodes
		long seed = 0;
		boolean dissemination = false;
		boolean altruism = false;

		Node[] nodes = new Node[parser.getNodesNumber()];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = new Epidemic(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i], 5000,
					100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), dissemination,
					altruism);
		}

		// run the trace
		List<Message> messages = Node.runTrace(nodes, parser.getTraceData(), true, dissemination, seed);
		estimatedTime = System.nanoTime() - startTime;

		System.out.println("Trace run duration: " + estimatedTime * 1e-9);
		System.out.println("Messages: " + messages.size());

		// print opportunistic algorithm statistics
		System.out.println(nodes[0].getName());
		System.out.print("" + Stats.computeHitRate(messages, nodes, dissemination) + ",");
		System.out.print("" + Stats.computeDeliveryCost(messages, nodes, dissemination) + ",");
		System.out.print("" + Stats.computeDeliveryLatency(messages, nodes, dissemination) + ",");
		System.out.println("" + Stats.computeHopCount(messages, nodes, dissemination));
	}
}
