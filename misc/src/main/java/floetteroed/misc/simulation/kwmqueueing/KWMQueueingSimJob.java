package floetteroed.misc.simulation.kwmqueueing;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public interface KWMQueueingSimJob {

	public String getId();

	public double getStartTime_s();

	public KWMQueueingSimLink getOriginLink();

	public KWMQueueingSimLink getDestinationLink();

	public KWMQueueingSimLink getNextLink();

	public KWMQueueingSimLink getCurrentLink();

	public void advanceToNextLink();

}
