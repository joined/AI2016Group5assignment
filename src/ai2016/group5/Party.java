package ai2016.group5;

import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.bidding.BidDetails;
import negotiator.AgentID;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;

import java.util.Arrays;
import java.util.HashMap;



public class Party {
	private AgentID id;
	public BidHistory bidHistory;
	private Integer nrIssues;
	private int[] issueIds;
	private EvaluatorDiscrete[] issues;

	/*
	 * Create a new Party object
	 */
	public Party(AgentID id, Bid example)
	{
		this.id = id;
		this.bidHistory = new BidHistory();
		this.nrIssues = example.getIssues().size();
		this.issueIds = new int[this.nrIssues];

		for (int i=0; i < example.getIssues().size(); i++)
		{
			this.issueIds[i] = example.getIssues().get(i).getNumber();
		}

		this.issues = new EvaluatorDiscrete[this.nrIssues];
		for (int i=0; i < this.nrIssues; i++)
		{
			this.issues[i] = new EvaluatorDiscrete();
		}
	}

	/*
	 * Add a bid to the bid history
	 */
	public void addBid(Bid bid,  double utility)
	{
		this.bidHistory.add(new BidDetails(bid,utility));
		this.setWeights();
	}
	
	/*
	 * Get the utility of the given bid
	 */
	public double getUtility(Bid bid)
	{
		double utility = 0.0;

		HashMap<Integer, Value> bidValues = bid.getValues();
		for (int i = 0; i < this.nrIssues; i++) {
			double weight = this.issues[i].getWeight();
			ValueDiscrete value = (ValueDiscrete)bidValues.get(this.issueIds[i]);
			if (((EvaluatorDiscrete)this.issues[i]).getValues().contains(value))
			{
				utility = utility + this.issues[i].getDoubleValue(value).doubleValue() * weight; 
			}
		}
		return utility;
	}
	/*
	 * Set the weights of the issues and the values per issue
	 */
	public void setWeights()
	{
		this.setWeightsIssues();
		this.setWeightsIssueValues();
	}

	/*
	 * Set the weights for the values of each issues using frequency analysis
	 */
	private void setWeightsIssueValues()
	{
		for (int i=0; i<this.nrIssues; i++)
		{
			HashMap<ValueDiscrete, Double> values = new HashMap<ValueDiscrete, Double>();
			for (int j=0; j<this.bidHistory.size(); j++)
			{
				ValueDiscrete value = (ValueDiscrete)(this.bidHistory.getHistory().get(j).getBid().getValue(this.issueIds[i]));
				if (values.containsKey(value))
				{
					values.put(value, values.get(value) + 1); 
				}
				else
				{
					values.put(value, 1.0);
				}
			}
			
			double max = 0.0;
			for (ValueDiscrete value : values.keySet())
			{
				if (values.get(value) > max) max = values.get(value);
			}
			for (ValueDiscrete value : values.keySet())
			{
				try {
					this.issues[i].setEvaluationDouble(value, values.get(value)/max);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/*
	 * Set the weights for each issue using frequency analysis
	 */

	private void setWeightsIssues()
	{
		int[] changesIssues = getChangesIssues();
		double[] weights = new double[this.nrIssues];
		Arrays.fill(weights, 1.0/this.nrIssues);
		double total = 0.0;

		for (int i=0; i<this.nrIssues; i++)
		{
			double w = weights[i] + (this.bidHistory.size() - changesIssues[i] -1)/10.0;
			this.issues[i].setWeight(w);
			total += w;
		}
		for (int i=0; i<this.nrIssues; i++)
		{
			this.issues[i].setWeight(this.issues[i].getWeight()/total);
		}
	}
	
	
	private int[] getChangesIssues()
	{
		return getChangesIssues(this.bidHistory.size());
	}
	/*
	 * Return an array which represents the frequency of change of each issue
	 */

	private int[] getChangesIssues(int rounds)
	{
		int[] changes = new int[this.nrIssues];
		for (int i=0; i<this.nrIssues; i++)
		{
			Value old = null;
			int count = 0;
			for (int j=this.bidHistory.size()-1; j>this.bidHistory.size()-rounds-1; j--)
			{	
				if (old != null)
				{
					if (!old.equals(this.bidHistory.getHistory().get(j).getBid().getValue(this.issueIds[i])))
					{
						count += 1;
					}
				}
				old = this.bidHistory.getHistory().get(j).getBid().getValue(this.issueIds[i]);
			}
			changes[i] = count;
		}
		return changes;
		
	}
	
	// Return how uniform the change in issues are. 1 = highest change is equal to smallest change, 0 = one issue does not change
	public Double uniformChangeInIssues(int rounds)
	{
		if (this.bidHistory.size() < rounds) return null;
		
		int[] changes = this.getChangesIssues(rounds);
		int max = 0;
		int min = this.bidHistory.size();
		for(int change : changes){
			if (change > max) max = change;
			if (change < min) min = change;
		}
		if (max == 0) return 0.0;		
		return min/(double)max;
	}
	
	// Return how hardHeaded the agent is. 0 = bids do not change in the last $rounds, 1 = bid change every time in the last $rounds
	public Double hardHeaded(int rounds)
	{
		if (this.bidHistory.size() < rounds) return null;
		
		int[] changes = this.getChangesIssues(rounds);
		int sum = 0;
		for (int change: changes){
			sum += change;
		}
		return (sum/(double)this.nrIssues)/(double)this.bidHistory.size();
		
		
	}

}
