package floetteroed.misc.simulation.kwmqueueing;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import floetteroed.utilities.networks.construction.AbstractLink;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class KWMQueueingSimLink extends AbstractLink<KWMQueueingSimNode, KWMQueueingSimLink> {

	// -------------------- CONSTANTS --------------------

	private final Random rnd;

	private ExponentialDistribution serviceDistribution = null;
	// private SingletonDistribution serviceDistribution = null;

	// TODO NEW
	private Map<KWMQueueingSimLink, UnivariateDistribution> outLink2turnServiceDistribution = new LinkedHashMap<>();

	private int spaceCapacity_jobs;

	private double fwdLag_s;

	private double bwdLag_s;

	// -------------------- STATE VARIABLES --------------------

	private int uqJobCnt;

	private LinkedList<KWMQueueingSimJob> runningJobs = new LinkedList<KWMQueueingSimJob>();

	private LinkedList<KWMQueueingSimJob> queueingJobs = new LinkedList<KWMQueueingSimJob>();

	private KWMQueueingSimLink blockingLink;

	private double blockingTime_s; // relevant only if blockingLink != null

	// -------------------- CONSTRUCTION --------------------

	public KWMQueueingSimLink(final String id, final Random rnd) {
		super(id);
		this.rnd = rnd;
		this.clear();
	}

	public KWMQueueingSimLink(final String id) {
		this(id, new Random());
	}

	public void clear() {
		this.uqJobCnt = 0;
		this.runningJobs.clear();
		this.queueingJobs.clear();
		this.blockingLink = null;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setServiceCapacity_jobs_s(final double serviceCapacity_jobs_s) {
		// this.serviceDistribution = new SingletonDistribution(1.0 /
		// serviceCapacity_jobs_s);
		this.serviceDistribution = new ExponentialDistribution(serviceCapacity_jobs_s, this.rnd);
	}

	public UnivariateDistribution getServiceDistribution() {
		return this.serviceDistribution;
	}

	// TODO NEW
	public UnivariateDistribution getServiceDistribution(final KWMQueueingSimLink toLink) {
		if (toLink == null) {
			return this.getServiceDistribution();
		} else {
			UnivariateDistribution result = this.outLink2turnServiceDistribution.get(toLink);
			if (result == null) {
				result = new ExponentialDistribution(
						Math.min(this.getServiceCapacity_jobs_s(), toLink.getServiceCapacity_jobs_s()), this.rnd);
				this.outLink2turnServiceDistribution.put(toLink, result);
			}
			return result;
		}
	}

	public double getServiceCapacity_jobs_s() {
		// return 1.0 / this.serviceDistribution.value;
		return this.serviceDistribution.getLambda();
	}

	public int getSpaceCapacity_jobs() {
		return spaceCapacity_jobs;
	}

	public void setSpaceCapacity_jobs(final int spaceCapacity_jobs) {
		this.spaceCapacity_jobs = spaceCapacity_jobs;
	}

	public double getFwdLag_s() {
		return fwdLag_s;
	}

	public void setFwdLag_s(final double fwdLag_s) {
		this.fwdLag_s = fwdLag_s;
	}

	public double getBwdLag_s() {
		return bwdLag_s;
	}

	public void setBwdLag_s(final double bwdLag_s) {
		this.bwdLag_s = bwdLag_s;
	}

	public void setUQJobCnt(final int uqJobCnt) {
		this.uqJobCnt = uqJobCnt;
	}

	// -------------------- SIMULATION --------------------

	// UPSTREAM QUEUE

	public int getUQJobCnt() {
		return uqJobCnt;
	}

	public void incrUQ() {
		this.uqJobCnt++;
	}

	public void decrUQ() {
		this.uqJobCnt--;
	}

	// RUNNING QUEUE

	public int getRQJobCnt() {
		return this.runningJobs.size();
	}

	public void addJobToRQ(final KWMQueueingSimJob job) {
		this.runningJobs.add(job);
	}

	public KWMQueueingSimJob getFirstJobFromRQ() {
		return this.runningJobs.getFirst();
	}

	public KWMQueueingSimJob removeFirstJobFromRQ() {
		return this.runningJobs.removeFirst();
	}

	// DOWNSTREAM QUEUE

	public int getDQJobCnt() {
		return this.queueingJobs.size();
	}

	public void addJobToDQ(final KWMQueueingSimJob job) {
		this.queueingJobs.add(job);
	}

	// GENERAL

	KWMQueueingSimJob removeFirstJobFromLink(final double time_s, final List<KWMQueueingSimEvent> newEvents) {
		final KWMQueueingSimJob result = this.queueingJobs.removeFirst();
		this.blockingLink = null; // this.setBlockingLink(null);
		newEvents.add(new KWMQueueingSimEvent(time_s + this.getBwdLag_s(), KWMQueueingSimEvent.TYPE.UQ_SPACE_ARR, this,
				null));
		if (this.getDQJobCnt() > 0) {
			// TODO NEW
			final KWMQueueingSimJob nextJob = this.queueingJobs.getFirst();
			newEvents.add(new KWMQueueingSimEvent(time_s + this.getServiceDistribution(nextJob.getNextLink()).next(),
					KWMQueueingSimEvent.TYPE.DQ_JOB_SERVICE, this, nextJob));
			// TODO ORIGINAL
			// newEvents.add(new KWMQueueingSimEvent(time_s +
			// this.getServiceDistribution().next(),
			// KWMQueueingSimEvent.TYPE.DQ_JOB_SERVICE, this,
			// this.queueingJobs.getFirst()));
		}
		return result;
	}

	public int getTotalJobCnt() {
		return this.getRQJobCnt() + this.getDQJobCnt();
	}

	public KWMQueueingSimLink getBlockingLink() {
		return this.blockingLink;
	}

	public double getBlockingTime_s() {
		return this.blockingTime_s;
	}

	public void setBlockingLinkAndTime_s(final KWMQueueingSimLink blockingLink, final double blockingTime_s) {
		this.blockingLink = blockingLink;
		this.blockingTime_s = blockingTime_s;
	}

	public boolean spillsBack() {
		return (this.uqJobCnt >= this.spaceCapacity_jobs);
	}
}
