/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu;

import java.util.*;

import mobemu.algorithms.Epidemic;
import mobemu.algorithms.MySprayAndWait;
import mobemu.algorithms.SprayAndFocus;
import mobemu.algorithms.SprayAndWait;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.Stats;
import mobemu.parsers.FestivalMobility;
import mobemu.parsers.Host;
import mobemu.parsers.SonarFestival;
import mobemu.parsers.ChatPair;
import mobemu.parsers.FMM;
import mobemu.trace.Parser;

class TraceTime {
	public long start = Long.MAX_VALUE;
	public long end = Long.MIN_VALUE;
}

/**
 * Main class for MobEmu.
 *
 * @author Radu
 */
public class MobEmu {

	public static void main(String[] args) {
		float gridHeight = 30f;
		float gridWidth = 20f;
		int rows = 6;
		int cols = 5;
		boolean showRun = false;
		int groupSize = 5;
		long startTime = System.nanoTime();
		// message average size 100kB, buffer capacity 500MB
		int dataMemory = 5000;
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
		FestivalMobility parser = new FestivalMobility(3600, 0.5f, 1f, 5.0f, 30.0f, 1.0f, gridHeight, rows,
				gridWidth, cols, 1.0f, groupSize, showRun, 10, 0);
//		 Parser parser = new HCMM(2 * 3600, 300, 0f, 0f, 0.1f, gridWidth, gridHeight, 10, 4, 10.0, 0.7,
//		 		 0.5f, 0.8f, 0, showRun, 10, false);
		// Parser parser = new SonarFestival();
		
		// determine start and end time of Sonar Festival trace using threads
		// vector of results for threads because using a mutex to update end and start time
		// would force threads to sleep too often => to many context switches
//		int noThreads = 8;
//		TraceTime traceTime[] = new TraceTime[noThreads];
//		Thread threads[] = new Thread[noThreads];
//		for (int i = 0; i < noThreads; i++) {
//			traceTime[i] = new TraceTime();
//			SonarFestivalTask myTask = new SonarFestivalTask((SonarFestival)parser, traceTime[i], i, noThreads);
//			threads[i] = new Thread(myTask);
//			threads[i].start();
//		}
//		for (int i = 0; i < noThreads; i++) {
//			try {
//				threads[i].join();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		// actually compute start and end time
//		long start = Long.MAX_VALUE;
//		long end = Long.MIN_VALUE;
//		for (int i = 0; i < noThreads; i++) {
//			System.out.println(traceTime[i].start + " " + traceTime[i].end);
//			if (start > traceTime[i].start) {
//				start = traceTime[i].start;
//			}
//			if (end < traceTime[i].end) {
//				end = traceTime[i].end;
//			}
//		}
//		parser.getTraceData().setStartTime(start);
//		parser.getTraceData().setEndTime(end);
//		System.out.println("start = " + start / Parser.MILLIS_PER_SECOND);
//		System.out.println("end = " + end / Parser.MILLIS_PER_SECOND);
		
		// Parser parser = new FMM();
		
		long estimatedTime = System.nanoTime() - startTime;
		startTime = System.nanoTime();
		System.out.println("Trace generation duration: " + estimatedTime * 1e-9);

		// print some trace statistics
		double duration = (double) (parser.getTraceData().getEndTime() - parser.getTraceData().getStartTime())
				/ (Parser.MILLIS_PER_MINUTE * 60);
		System.out.println("Trace duration in hours: " + duration);
		System.out.println("Trace start " + parser.getTraceData().getStartTime());
		System.out.println("Trace end " + parser.getTraceData().getEndTime());
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
//			nodes[i] = new Epidemic(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i], dataMemory,
//					100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), dissemination,
//					altruism);
//			nodes[i] = new SprayAndWait(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i], 5000,
//					100, seed,parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), dissemination, altruism,
//					SprayAndWait.Type.BINARY);
//			nodes[i] = new SprayAndFocus(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i], 5000,
//					100, seed,parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), altruism,
//					Node.MILLIS_IN_10MIN);
			nodes[i] = new MySprayAndWait(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i], 5000,
					100, seed,parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), dissemination, altruism);
			nodes[i].setCommonFriends(parser.getCommonFriends(i));
			System.out.println(parser.getCommonFriends(i).toString());
		}

		System.out.println("Generated nodes");
			
		// run the trace
		System.out.println("Run simulation");
		List<Message> messages = Node.runTrace(nodes, parser.getTraceData(), true, dissemination, seed);
		estimatedTime = System.nanoTime() - startTime;
		
		System.out.println("Trace run duration: " + estimatedTime * 1e-9);
		System.out.println("Messages: " + messages.size());

		// print opportunistic algorithm statistics
		System.out.println(nodes[0].getName());
		System.out.println("Hit rate = " + Stats.computeHitRate(messages, nodes, dissemination) + ",");
		System.out.println("Delivery cost = " + Stats.computeDeliveryCost(messages, nodes, dissemination) + ",");
		System.out.println("Delivery latency = " + Stats.computeDeliveryLatency(messages, nodes, dissemination) + ",");
		System.out.println("Hop count = " + Stats.computeHopCount(messages, nodes, dissemination));
	}
}
