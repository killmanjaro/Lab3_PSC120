package khModel;

import sim.util.Bag;
import spaces.Spaces;
import sweep.SimStateSweep;

public class Environment extends SimStateSweep {


	public double active = 1.0;//probability of activity
	public double p = 1.0; //random movement
	public boolean oneCellPerAgent = false;//controls whether agents can occupy the same place or not
	public double aggregate = 0.0;//probability of aggregating
	public int searchRadius = 2;// the radius or view of an agent when aggregating

	/*
	 * KH Model parameters
	 */
	//TODO: declare a boolean variable nonSpatialModel with a getter-setter pair
	//TODO:also a double maxFrustration variable with a getter-setter pair and set it to 50
	//TODO: an int dateSearchRadius with a getter-setter pair
	public int males = 1000;//size of the population of males
    public int females = 1000;
    public double maxAttractiveness = 10.0;//maximum attractiveness
    public double choosiness =3.0;//exponent in rule for calculating probabilities
    public double maxDates = 50;//maximum number of dates.
    public Rule rule= Rule.ATTRACTIVE;//Constant for the decision rules
    public int ruleNumber = 0;
    Bag male = new Bag();//the population males
    Bag female = new Bag();//the current population of females
    Bag nextMale = new Bag();//population of males for next step 
    Bag nextFemale = new Bag();
    public Experimenter experimenter = null;
	public int dateSearchRadius = 1;
    
	
    public Environment(long seed, Class observer) {
		super(seed, observer);
	}
    
    /**
     * Reset global variables
     */
    public void reset() {
    	male.clear();
    	female.clear();
    	nextMale.clear();
    	nextFemale.clear();
    }

	//TODO: getters and setters for new variable


	public int getMales() {
		return males;
	}

	public void setMales(int males) {
		this.males = males;
	}

	public int getFemales() {
		return females;
	}

	public void setFemales(int females) {
		this.females = females;
	}

	public double getMaxAttractiveness() {
		return maxAttractiveness;
	}

	public void setMaxAttractiveness(double maxAttractiveness) {
		this.maxAttractiveness = maxAttractiveness;
	}

	public double getChoosiness() {
		return choosiness;
	}

	public void setChoosiness(double choosiness) {
		this.choosiness = choosiness;
	}

	public double getMaxDates() {
		return maxDates;
	}

	public void setMaxDates(double maxDates) {
		this.maxDates = maxDates;
	}

	public int getRuleNumber() {
		return ruleNumber;
	}

	public void setRuleNumber(int ruleNumber) {
		this.ruleNumber = ruleNumber;
		this.rule = (Rule.values())[ruleNumber];
	}

	public Object domRuleNumber() {
		return Rule.values();
	}

	public int getGridWidth() {
		return gridWidth;
	}

	public void setGridWidth(int gridWidth) {
		this.gridWidth = gridWidth;
	}

	public int getGridHeight() {
		return gridHeight;
	}

	public void setGridHeight(int gridHeight) {
		this.gridHeight = gridHeight;
	}

	public double getActive() {
		return active;
	}

	public void setActive(double active) {
		this.active = active;
	}

	public double getP() {
		return p;
	}

	public void setP(double p) {
		this.p = p;
	}

	public boolean isOneCellPerAgent() {
		return oneCellPerAgent;
	}

	public void setOneCellPerAgent(boolean oneCellPerAgent) {
		this.oneCellPerAgent = oneCellPerAgent;
	}

	public double getAggregate() {
		return aggregate;
	}

	public void setAggregate(double aggregate) {
		this.aggregate = aggregate;
	}

	public int getSearchRadius() {
		return searchRadius;
	}

	public void setSearchRadius(int searchRadius) {
		this.searchRadius = searchRadius;
	}

	/**
	 * Creates agents and places them in space and on the schedule.
	 */
	
	public void makeAgent() {
		if(this.oneCellPerAgent) {
			if(females+males > gridWidth*gridHeight) {
				System.out.println("Too many agents reduce to a total of "+ gridWidth*gridHeight);
			}
				
		}
		//TODO: Almost the same as the makeAgentNonSpatial below except you will use the 
		//constructor: Agent(int x, int y, int dirx, int diry, boolean female, double attractiveness)
		//don't forget xdir and ydir
		//Hint: look at the code-along lab for generating xdir and ydir
		
		for(int i=0;i<females;i++) {
			int x = random.nextInt(gridWidth);
			int y = random.nextInt(gridHeight);
			if(oneCellPerAgent) {
				Bag b = sparseSpace.getObjectsAtLocation(x, y);
				while(b != null) {
					x = random.nextInt(gridWidth);
					y = random.nextInt(gridHeight);
					b = sparseSpace.getObjectsAtLocation(x, y);
				}
			}
			double attractiveness = random.nextInt((int)maxAttractiveness)+1;
			Agent f = new Agent(x, y, true,attractiveness);
			f.event = schedule.scheduleRepeating(f);
			sparseSpace.setObjectLocation(f,x, y);
			gui.setOvalPortrayal2DColor(f, (float)1, (float)0, (float)0, (float)(attractiveness/maxAttractiveness));
			female.add(f);
		}
		for(int i=0;i<males;i++) {
			int x = random.nextInt(gridWidth);
			int y = random.nextInt(gridHeight);
			if(oneCellPerAgent) {
				Bag b = sparseSpace.getObjectsAtLocation(x, y);
				while(b != null) {
					x = random.nextInt(gridWidth);
					y = random.nextInt(gridHeight);
					b = sparseSpace.getObjectsAtLocation(x, y);
				}
			}
			double attractiveness = random.nextInt((int)maxAttractiveness)+1;
			Agent m = new Agent(x, y, false,attractiveness);
			m.event = schedule.scheduleRepeating(m);
			sparseSpace.setObjectLocation(m,x, y);
			gui.setOvalPortrayal2DColor(m, (float)0, (float)0, (float)1, (float)(attractiveness/maxAttractiveness));
			male.add(m);
		}
	}
	

	public void start() {
		super.start();
		reset();
		this.make2DSpace(Spaces.SPARSE, gridWidth, gridHeight);
		makeAgent();
		if(observer != null) {
			observer.initialize(sparseSpace, Spaces.SPARSE);//initialize the experimenter by calling initialize in the parent class
			experimenter = (Experimenter)observer;//cast observer as experimenter
			experimenter.resetVariables();
		}
	}
}
