package mobemu.algorithms;

import java.util.ArrayList;
import java.util.List;

import mobemu.algorithms.SprayAndWait.Type;
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;

public class MySprayAndWait extends Node {
    private final boolean dissemination;
    private final boolean altruismAnalysis;

    /**
     * Instantiates a {@code SprayAndWait} object.
     *
     * @param id ID of the node
     * @param nodes total number of existing nodes
     * @param context the context of this node
     * @param socialNetwork the social network as seen by this node
     * @param dataMemorySize the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     * history
     * @param seed the seed for the random number generators
     * @param traceStart timestamp of the start of the trace
     * @param traceEnd timestamp of the end of the trace
     * @param dissemination {@code true} if dissemination is used, {@code false}
     * if routing is used
     * @param type type of the Spray and Wait algorithm (source or binary)
     * @param altruism {@code true} if altruism computations are performed,
     * {@code false} otherwise
     */
    public MySprayAndWait(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, boolean dissemination, boolean altruism) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.dissemination = dissemination;
        this.altruismAnalysis = altruism;
    }

    @Override
    public String getName() {
        return "My Spray and Wait";
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof MySprayAndWait)) {
            return;
        }

        MySprayAndWait sprayAndWaitEncounteredNode = (MySprayAndWait) encounteredNode;
        int remainingMessages = deliverDirectMessages(sprayAndWaitEncounteredNode, altruismAnalysis, contactDuration, currentTime, dissemination);
        int totalMessages = 0;
        List<Message> toRemove = new ArrayList<>();

        // download each message in the encountered node's data memory that is not in the current node's memory
        for (Message message : sprayAndWaitEncounteredNode.dataMemory) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (!runSprayAndWait(message, sprayAndWaitEncounteredNode, toRemove)) {
                continue;
            }

            if (insertMessage(message, sprayAndWaitEncounteredNode, currentTime, altruismAnalysis, dissemination)) {
                totalMessages++;
            }
        }

        // download each message generated by the encountered node that is not in the current node's memory
        for (Message message : sprayAndWaitEncounteredNode.ownMessages) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (!runSprayAndWait(message, sprayAndWaitEncounteredNode, toRemove)) {
                continue;
            }

            if (insertMessage(message, sprayAndWaitEncounteredNode, currentTime, altruismAnalysis, dissemination)) {
                totalMessages++;
            }
        }

        for (Message message : toRemove) {
            sprayAndWaitEncounteredNode.removeMessage(message, false);
        }
        toRemove.clear();
    }

    /**
     * My Spray and Wait algorithm.
     *
     * @param message message to be analyzed
     * @param encounteredId ID of the encountered node
     * @return {@code true} if the message should be copied, {@code false}
     * otherwise
     */
    private boolean runSprayAndWait(Message message, MySprayAndWait encounteredNode, List<Message> toRemove) {
    	int encounteredId = encounteredNode.getId();
    	int dest = message.getDestination();

        if (message.getCopies(encounteredNode.getId()) == 1) {
            // if the node has a social relationship with the destination, pass the message
//        	if (!encounteredNode.socialNetwork[message.getDestination()]) {
//        		if (this.socialNetwork[message.getDestination()]) {
//        			toRemove.add(message);
//                    return true;
//                 }
//        	}
            
            // if the node has more friends in common with the destination, it is possible
            // to be later attracted to the same cell, so pass the message
        	System.out.println(id + " " + this.commonFriends[dest] + " " + encounteredId + " " +
        			encounteredNode.commonFriends[dest]);
        	if (this.commonFriends[dest] > encounteredNode.commonFriends[dest]) {
        		toRemove.add(message);
        		return true;
        	}
            
            // if this node has higher centrality

            return false;
        }

        // if the current node doesn't contain the message, it receives half of the copies
        if (!dataMemory.contains(message) && !ownMessages.contains(message)) {
            message.setCopies(encounteredId, message.getCopies(encounteredId) / 2);
            message.setCopies(id, message.getCopies(encounteredId));
        }

        return true;
    }

}
