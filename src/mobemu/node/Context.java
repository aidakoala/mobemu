/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Class representing an opportunistic node's context information.
 *
 * @author Radu
 */
public class Context {

    private int id; // id of the node whose context this is
    private Set<Topic> topics; // topics belonging to this context
    private static int maxTopicsNo = 0;
    private static int maxTopicId = -1;
    public static final int MESSAGE_CONTEXT = -1;

    /**
     * Instantiates a {@code Context} object.
     *
     * @param id ID of the node this context belongs to
     */
    public Context(int id) {
        this.id = id;
        this.topics = new HashSet<>();
    }

    /**
     * Instantiates a message {@code Context} object.
     */
    public Context() {
        this(MESSAGE_CONTEXT);
    }

    /**
     * Gets the ID of the node whose context this is.
     *
     * @return the ID of the node
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the topics of this context.
     *
     * @return a set of topics belonging to this context
     */
    public Set<Topic> getTopics() {
        return topics;
    }

    /**
     * Add the topics to this context.
     *
     * @param topic topic to be added
     */
    public void addTopic(Topic topic) {
        topics.add(topic);
        updateMaxTopics();
    }

    /**
     * Adds a topic set to this context.
     *
     * @param topicSet topic set to be added
     */
    public void addTopicSet(Set<Topic> topicSet) {
        topics.addAll(topicSet);
        updateMaxTopics();
    }

    /**
     * Returns the number of common topics between two contexts.
     *
     * @param otherContext context of the node that we are comparing to
     * @param time time the comparison is made at
     * @return number of common topics
     */
    public int getCommonTopics(Context otherContext, long time) {
        int common = 0;

        for (Topic listItem : topics) {
            if (Topic.isTopicCommon(otherContext.topics, listItem, time)) {
                common++;
            }
        }

        return common;
    }

    /**
     * Gets the number of topics of a context.
     *
     * @param time time the request is made at
     * @return number of topics in the context
     */
    public int getNumberOfTopics(long time) {
        int total = 0;

        for (Topic listItem : topics) {
            if (listItem.getTime() <= time) {
                total++;
            }
        }

        return total;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Context) {
            Context other = (Context) obj;

            if (other.topics.size() != topics.size()) {
                return false;
            }

            for (Topic topic : topics) {
                if (!other.topics.contains(topic)) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + this.id;
        hash = 97 * hash + Objects.hashCode(this.topics);
        return hash;
    }

    /**
     * Gets the maximum number of topics.
     *
     * @return maximum number of topics
     */
    public static int getMaxTopicsNumber() {
        return maxTopicsNo;
    }

    /**
     * Gets the maximum topic ID.
     *
     * @return maximum topic ID
     */
    public static int getMaxTopicId() {
        return maxTopicId;
    }

    /**
     * Resets the static data of the context.
     */
    public static void reset() {
        maxTopicsNo = 0;
        maxTopicId = -1;
    }

    /**
     * Gets the context item corresponding to the given id from a context list.
     *
     * @param context list of context items
     * @param id ID of the node whose context is required
     * @return context item corresponding to the ID
     */
    public static Context getContextItem(List<Context> context, int id) {
        Context contextItem = null;

        for (Context ctx : context) {
            if (ctx.id == id) {
                contextItem = ctx;
                break;
            }
        }

        if (contextItem == null) {
            contextItem = new Context(id);
            context.add(contextItem);
        }

        return contextItem;
    }

    /**
     * Updates the maximum number of topics.
     */
    private void updateMaxTopics() {
        int current = 0;

        current += topics.size();

        if (current > maxTopicsNo) {
            maxTopicsNo = current;
        }

        for (Topic topic : topics) {
            if (topic.getTopic() > maxTopicId) {
                maxTopicId = topic.getTopic();
            }
        }
    }
}
