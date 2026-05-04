package khModel;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.util.Bag;

public class Agent implements Steppable {
	int x;//x,y coordinates
	int y;
	int dirx;//the direction of movement
	int diry;
	//KH model
	public boolean female;//determines whether an agent is a female == true, male == false
	public double attractiveness;//Attractiveness of this agent
	//TODO: add a double frustration = 1 variable
	public double dates = 1;//starts with 1 and incremented by 1 with each date.
	public boolean dated = false;//flag for dating on each round
	public Stoppable event;//allows to remove an agent from the simulation.


	public Agent(int x, int y, boolean female, double attractiveness) {
		super();
		this.x = x;
		this.y = y;
		this.female = female;
		this.attractiveness = attractiveness;
	}
	
	public Agent(int x, int y, int dirx, int diry, boolean female, double attractiveness) {
		super();
		this.x = x;
		this.y = y;
		this.dirx = dirx;
		this.diry = diry;
		this.female = female;
		this.attractiveness = attractiveness;
	}

	/**
	 * Finds a date for an agent of the opposite sex randomly from the male or female populations.
	 * @param state
	 * @return
	 */
	public Agent findDate(Environment state) {
		if(female ) {//agent gender
			if(state.male.numObjs==0)
				return null;//if empty return null
			return (Agent)state.male.objs[state.random.nextInt(state.male.numObjs)];
		}
		else {
			if(state.female.numObjs==0)
				return null;
			return (Agent)state.female.objs[state.random.nextInt(state.female.numObjs)];
		}
	}

	/**
	 * This method finds a date from the local neighborhood in space of this agent.
	 * @param state
	 * @return
	 */

	public Agent findLocalDate(Environment state) {
		Bag neighbors = state.sparseSpace.getMooreNeighbors(x, y, state.dateSearchRadius, state.sparseSpace.TOROIDAL, true);
		// draw agents randomly from the neighbors bag if it is not empty.  If an agent is of the opposite sex and not dated, then
		//return it.
		if (neighbors.numObjs > 0) {
			for(int i=0;i<neighbors.numObjs;i++) {
				Agent a = (Agent)neighbors.objs[i];
				// check if opposite sex/ not dated
				if (female ) {
					// if a is a male
					if (a.female == false) {
						return a;
					}
				}
				else {
					if (a.female == true ) {
						return a;
					}
				}
			}
		}
		return null; //a place holder for when you will return an agent that has not dated or null if none can be found or all of dated
	}
	public void replicate (Environment state, boolean gender){
		  //Your code here. It should do the following. 
	      //(1) Create a new agent (a spatial agent will work 
	      //for non-spatial as well)
	      //Agent(int x, int y, int dirx, int diry, 
	      //  boolean female, double attractiveness)
	      //(2) initialize it with the same gender (to make sure 
	      //the gender ratio remains constant),
	      //(3) give it a new random attractiveness score, and 
	      //(4) schedule it
		 //a.event = state.schedule.scheduleRepeating(a); 
	      //(5) create a new graphic for the agent:
	      //state.gui.setOvalPortrayal2DColor(a, (float)0, (float)0, (float)1, 
         //  (float)(attractiveness/state.maxAttractiveness));
	     //Hint: How did we make gender agents in the Environment?  
	    
	}

	/**
	 * KH closing time rule
	 * @param state
	 * @param p
	 * @return
	 */
	public double ctRule(Environment state, double p) {
		return Math.pow(p, ct(state));
	}
	
	public double ct(Environment state) {
		if (state.maxDates+1-dates >= 0)
			return (state.maxDates+1-dates)/(state.maxDates);
		else
			return 0.0;
	}
	/**
	 * Attractiveness rule
	 * @param state
	 * @param a
	 * @return
	 */
	public double p1(Environment state, Agent a) {
		return Math.pow(a.attractiveness, state.choosiness)/Math.pow(state.maxAttractiveness, state.choosiness);
	}

	public double p2(Environment state, Agent a) {
		return Math.pow(state.maxAttractiveness-Math.abs(this.attractiveness - a.attractiveness), state.choosiness)/
				Math.pow(state.maxAttractiveness, state.choosiness);
	}

	/**
	 * Mixed rule
	 * @param state
	 * @param a
	 * @return
	 */
	public double p3(Environment state, Agent a) {
		return (p1(state,a)+p2(state,a))/2.0;
	}
	

	/**
	 * Implements the mixed rule 2 or frustration rule;
	 * @param state
	 * @param a
	 * @return
	 */

	public double p4(Environment state, Agent a) {
		//TODO
		return 0.0; //when finished this should return a probability you calculate
	}
	
	public double frRule(Environment state) {
		//TODO: Implement the FR step function
		return 0;//replace with appropriate code
	}

	public void remove(Environment state) {
		if(female) 
			state.female.remove(this);//remove from the population
		else
			state.male.remove(this);
		state.sparseSpace.remove(this);//remove it from space
		event.stop();//remove from the schedule
	}

	public void nextPopulationStep(Environment state) {
		dated = true; //set dated to true.
		if(female) {
			state.nextFemale.add(this);
			state.female.remove(this);
		}
		else {
			state.nextMale.add(this);
			state.male.remove(this);
		}
	}
	
	/**
	 * Whether a simulation is spatial or non-spatial, once given an Agent a (partner), it 
	 * handles the mutual discision process.
	 * @param state
	 * @param a
	 */
	public void date(Environment state, Agent a) {
		double p;
		double q;
		switch(state.rule) {
		case ATTRACTIVE:
			p = p1(state,a);
			q = a.p1(state, this);
			break;
		case SIMILAR:
			p = p2(state,a);
			q = a.p2(state, this);
			break;
		case MIXED:
			p = p3(state,a);
			q = a.p3(state, this);
			break;
		//TODO: add a case for the p4 rule
		//TODO: only if you do the extra credit, case for you p5 rule
		default:
			p = p1(state,a);
			q = a.p1(state, this);
			break;	
		}
		p = ctRule(state,p);
		q = ctRule(state,q);

		if(state.random.nextBoolean(p)&& state.random.nextBoolean(q)) {//couple decison
			if(female) {
				state.experimenter.getData(this, a);
			}
			else {
				state.experimenter.getData(a, this);
			}
			remove(state);
			a.remove(state);
			//TODO: When replacement is checked, code is needed here to handle replacement
		} //end if test
		else {
			this.nextPopulationStep(state);
			a.nextPopulationStep(state);
		}
		if(dates <= state.maxDates) {
			dates++;
		}
		if(a.dates <= state.maxDates) {
			a.dates++;
		}
		//TODO: update frustration
	}

	/**
	 * Handles dates for non-spatial and spatial models. It finds a partner
	 * for spatial and non-spatial simulations and then uses date(Environment state, Agent a)
	 * @param state
	 */
	public void date(Environment state) {
		Agent a = findDate(state);
		if(a != null)
			date(state, a);
		//TODO: replace the above code with an if than conditional
		//of the form:
		//if(state.nonSpatialModel) {
		//  Agent a = findDate(state);
		//  if(a != null)
		//	  date(state, a);
		//	}
		//}
		//else {
		//	Similar to the above using findLocalDate
		//}
	}

	public void placeAgent(Environment state) {
		//TODO: See code-along lab
		/*
		 * This requires basically the same code as the placeAgent
		 * method in the code-along lab, where we must consider
		 * whether there is oneCellPerAgent or many are allowed.
		 */
		
	}
	/**
	 * Agents move randomly to a new location for either one agent per cell or possibly
	 * multiple agents per cell.
	 * @param state
	 */
	public void move(Environment state) {
		//TODO: See code-along lab
		/*
		 * This method is basically the same as the move method
		 * in the code-along lab
		 */
		
	}


	public int decideX(Environment state, Bag neighbors) {
		//TODO: See code-along lab
		/*
		 * This is basically the same code as in the code-along lab
		 * (1) find the number of posX and negX agents to either side
		 * of an agent, then use if then statements to determine whether 
		 * to return 1, 0, -1
		 * 
		 */
		
	}

	public int decideY(Environment state, Bag neighbors) {
		//TODO: See code-along lab and decideX
		
	}

	public void aggregate (Environment state) {
		//TODO: See code-along lab
		/*
		 * This is basically the same as the code-along lab. First get the neighbors.
		 * then calculated dirx and diry,
		 * and finally place the agents
		 */
		
	}

	public void step(SimState state) {
		Environment environment = (Environment)state;
		//TODO: see code-along lab
		//You will have to write code that does
		/*
		if(random probability to aggregate) {
			aggregate
		}
		else {
			move;
		}
		 */

		if(!dated)
			date(environment);

	}

}
