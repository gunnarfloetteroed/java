package floetteroed.misc.simulation.kwmqueueing.examples;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import floetteroed.misc.simulation.eventbased.AbstractEventHandler;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimEvent;
import floetteroed.utilities.Triple;


public class TurnCounter extends AbstractEventHandler<KWMQueueingSimEvent>{
	
		
		// -------------------- MEMBERS --------------------

		private final Map<Triple<String, String, Integer>, Long> turns = new LinkedHashMap<Triple<String, String, Integer>, Long>();
		
		// -------------------- CONSTRUCTION --------------------

		public TurnCounter() {
		}
       
		//--------------------SIMPLE GETTERS-----------------------
		public long getCount(final String fromLinkId, final String toLinkId, final int time_step) {
			final Triple<String, String, Integer> key = new Triple<String, String, Integer>(fromLinkId,
					toLinkId, time_step);
			if (this.turns.containsKey(key)) {
				return this.turns.get(new Triple<String, String, Integer>(fromLinkId,
						toLinkId, time_step));
			} else {
				return 0l;
			}
		}

		// --------------- IMPLEMENTATION OF AbstractEventHandler ---------------

		@Override
		public boolean isResponsible(final KWMQueueingSimEvent event) {
			return (KWMQueueingSimEvent.TYPE.UQ_JOB_ARR.equals(event.getType())
					&& event.getJob() != null
					&& event.getJob().getCurrentLink() != null
					&& event.getJob().getNextLink() != null);
		}

		@Override
		public List<KWMQueueingSimEvent> process(final KWMQueueingSimEvent event) {
			final int time_step = (int) Math.ceil(event.getTime_s());
			final Triple<String, String, Integer> key = new Triple<String, String, Integer>(event
					.getJob().getCurrentLink().getId(), event.getJob()
					.getNextLink().getId(), time_step);
			Long cnt = this.turns.get(key);
			this.turns.put(key, (cnt == null ? 1l : cnt + 1l));
			return null;
		}

		// -------------------- OVERRIDING OF Object --------------------

		@Override
		public String toString() {
			final StringBuffer result = new StringBuffer();
			for (Map.Entry<Triple<String, String, Integer>, Long> entry : this.turns
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
