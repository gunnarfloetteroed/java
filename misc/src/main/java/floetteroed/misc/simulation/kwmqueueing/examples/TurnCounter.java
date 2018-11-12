package floetteroed.misc.simulation.kwmqueueing.examples;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import floetteroed.misc.simulation.eventbased.AbstractEventHandler;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimEvent;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimJob;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;
import floetteroed.utilities.Triple;


public class TurnCounter extends AbstractEventHandler<KWMQueueingSimEvent>{
	
		
		// -------------------- MEMBERS --------------------

		private Map<Triple<String, String, Integer>, Integer> turns;
		
		// -------------------- CONSTRUCTION --------------------

		public TurnCounter() {
			turns = new LinkedHashMap<Triple<String, String, Integer>, Integer>();
		}
       
		//--------------------SIMPLE GETTERS-----------------------
		public int getCount(final String fromLinkId, final String toLinkId, final int time_step) {
			final Triple<String, String, Integer> key = new Triple<String, String, Integer>(fromLinkId,
					toLinkId, time_step);
			if (this.turns.containsKey(key)) {
				return this.turns.get(new Triple<String, String, Integer>(fromLinkId,
						toLinkId, time_step));
			} else {
				return 0;
			}
		}

		// --------------- IMPLEMENTATION OF AbstractEventHandler ---------------

		@Override
		public boolean isResponsible(final KWMQueueingSimEvent event) {
			return (KWMQueueingSimEvent.TYPE.DQ_JOB_FLOW.equals(event.getType())
				   );
		}

		@Override
		public List<KWMQueueingSimEvent> process(final KWMQueueingSimEvent event) throws RuntimeException {

			final List<KWMQueueingSimEvent> newEvents = new LinkedList<KWMQueueingSimEvent>();
			final double time_s = event.getTime_s();
			final KWMQueueingSimLink link = event.getLink();
			final KWMQueueingSimJob job = event.getJob();
			final KWMQueueingSimLink nextLink = job.getNextLink();
			
			final int time_step = (int) Math.ceil(time_s);
			final Triple<String, String, Integer> key = new Triple<String, String, Integer>(link.getId(), nextLink.getId(), time_step);
			Integer cnt = this.turns.get(key);
			this.turns.put(key, (cnt == null ? 1 : cnt + 1));
			
			newEvents.add(new KWMQueueingSimEvent(time_s,
					KWMQueueingSimEvent.TYPE.UQ_JOB_ARR, nextLink, job));

			return newEvents;
		}

		// -------------------- OVERRIDING OF Object --------------------

		@Override
		public String toString() {
			final StringBuffer result = new StringBuffer();
			for (Map.Entry<Triple<String, String, Integer>, Integer> entry : this.turns
					.entrySet()) {
				result.append(entry.getKey().getA());
				result.append(" --> ");
				result.append(entry.getKey().getB());
				result.append(" during time step ");
				result.append(entry.getKey().getC());
				result.append(" : ");
				result.append(entry.getValue());
				result.append("\n");
			}

			return result.toString();
		}

}
