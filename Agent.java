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
	public double frustration = 1;//starts at 1, incremented each unsuccessful date
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
		neighbors.shuffle(state.random); //randomize bag BEFORE iterating so selection is random
		if (neighbors.numObjs > 0) {
			for(int i=0;i<neighbors.numObjs;i++) {
				Agent a = (Agent)neighbors.objs[i];
				// check if opposite sex/ not dated
				if (female ) {
					// if a is a male
					if (a.female == false) {
						if (a.dated == false) { //check partner's dated flag (bug fix: was checking self)
							return a;
						}
					}
				}	
				else {
					if (a.female == true ) {
						if (a.dated == false) { //check partner's dated flag (bug fix: was checking self)
							return a;
						}
					}
				}
			}
		}
		return null; //return null if no eligible undated opposite-sex neighbor found
	}

	public void replicate (Environment state, boolean gender){
		// (1) Create a new spatial agent with random position and direction
		int nx = state.random.nextInt(state.gridWidth);
		int ny = state.random.nextInt(state.gridHeight);
		int ndirx = state.random.nextInt(3) - 1; // -1, 0, or 1
		int ndiry = state.random.nextInt(3) - 1;
		// (3) give it a new random attractiveness score
		double newAttractiveness = state.random.nextInt((int)state.maxAttractiveness) + 1;
		// (1)+(2) same gender as parameter
		Agent a = new Agent(nx, ny, ndirx, ndiry, gender, newAttractiveness);
		// (4) schedule it
		a.event = state.schedule.scheduleRepeating(a);
		state.sparseSpace.setObjectLocation(a, nx, ny);
		// (5) create graphic and add to the correct population bag
		if(gender) { // female -> red
			state.gui.setOvalPortrayal2DColor(a, (float)1, (float)0, (float)0,
					(float)(newAttractiveness/state.maxAttractiveness));
			state.female.add(a);
		} else { // male -> blue
			state.gui.setOvalPortrayal2DColor(a, (float)0, (float)0, (float)1,
					(float)(newAttractiveness/state.maxAttractiveness));
			state.male.add(a);
		}
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
	 * Implements the frustration rule p4:
	 * p4 = F*p1 + (1-F)*p2
	 * Agents initially prefer attractiveness (p1); as frustration grows
	 * they gradually shift toward preferring similarity (p2).
	 * @param state
	 * @param a
	 * @return probability
	 */
	public double p4(Environment state, Agent a) {
		double F = frRule(state);
		return F * p1(state, a) + (1 - F) * p2(state, a);
	}
	
	/**
	 * F step function used by p4.
	 * F = (fmax + 1 - f) / fmax  when f <= fmax
	 * F = 0                       otherwise
	 * At frustration=1 (first date) F=1 so agent uses p1.
	 * At frustration=fmax, F→0 so agent uses p2.
	 * @param state
	 * @return F in [0,1]
	 */
	public double frRule(Environment state) {
		if(frustration <= state.maxFrustration) {
			return (state.maxFrustration + 1 - frustration) / state.maxFrustration;
		} else {
			return 0.0;
		}
	}

	/**
	 * Extra Credit Rule p5: Choosiness-Weighted blend.
	 * W = choosiness / (choosiness + 1), so the weight on attractiveness (p1)
	 * grows with choosiness while the weight on similarity (p2) shrinks.
	 * Unlike p3 (fixed 50/50) this lets the parameter choosiness control
	 * which signal dominates, independently of the exponent effect already
	 * built into p1 and p2.  High choosiness → nearly pure p1;
	 * choosiness → 0 → nearly pure p2.
	 * @param state
	 * @param a
	 * @return probability
	 */
	public double p5(Environment state, Agent a) {
		double W = state.choosiness / (state.choosiness + 1.0); // in (0,1)
		return W * p1(state, a) + (1.0 - W) * p2(state, a);
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
	 * handles the mutual decision process.
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
		case FRUSTRATION: //p4 frustration-based mixed rule
			p = p4(state,a);
			q = a.p4(state, this);
			break;
		case WEIGHTED: //extra credit p5 choosiness-weighted rule
			p = p5(state,a);
			q = a.p5(state, this);
			break;
		default:
			p = p1(state,a);
			q = a.p1(state, this);
			break;	
		}
		p = ctRule(state,p);
		q = ctRule(state,q);

		if(state.random.nextBoolean(p)&& state.random.nextBoolean(q)) {//couple decision
			if(female) {
				state.experimenter.getData(this, a);
			}
			else {
				state.experimenter.getData(a, this);
			}
			remove(state);
			a.remove(state);
			//When replacement is checked, replace the removed male and female
			if(state.replacement) {
				replicate(state, true);  //new female
				replicate(state, false); //new male
			}
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
		//update frustration for both agents after each date attempt
		if(frustration <= state.maxFrustration) {
			frustration++;
		}
		if(a.frustration <= state.maxFrustration) {
			a.frustration++;
		}
	}

	/**
	 * Handles dates for non-spatial and spatial models. It finds a partner
	 * for spatial and non-spatial simulations and then uses date(Environment state, Agent a)
	 * @param state
	 */
	public void date(Environment state) {
		//switch between spatial & non-spatial model
		if(state.nonSpatialModel) {
			Agent a = findDate(state); //non-spatial (aka original) model uses findDate
	        if(a != null)
	            date(state, a);
		}
        else {
        	Agent a = findLocalDate(state); //spatial model uses findLocalDate
	        if(a != null)
	            date(state, a);
        }
	}

	public void placeAgent(Environment state) {
		if(state.oneCellPerAgent) {
			//find a free cell; keep re-drawing if occupied by another agent
			Bag b = state.sparseSpace.getObjectsAtLocation(x, y);
			while(b != null && b.numObjs > 0 && !b.contains(this)) {
				x = state.random.nextInt(state.gridWidth);
				y = state.random.nextInt(state.gridHeight);
				b = state.sparseSpace.getObjectsAtLocation(x, y);
			}
		}
		state.sparseSpace.setObjectLocation(this, x, y);
	}

	/**
	 * Agents move randomly to a new location for either one agent per cell or possibly
	 * multiple agents per cell.
	 * @param state
	 */
	public void move(Environment state) {
		//pick a random direction offset: -1, 0, or 1 in each axis
		int newDirX = state.random.nextInt(3) - 1;
		int newDirY = state.random.nextInt(3) - 1;
		//toroidal wrap
		int newX = (x + newDirX + state.gridWidth)  % state.gridWidth;
		int newY = (y + newDirY + state.gridHeight) % state.gridHeight;

		if(state.oneCellPerAgent) {
			Bag b = state.sparseSpace.getObjectsAtLocation(newX, newY);
			if(b == null || b.numObjs == 0) { //only move if destination is free
				state.sparseSpace.remove(this);
				x = newX;
				y = newY;
				placeAgent(state);
			}
			//else stay put
		} else {
			state.sparseSpace.remove(this);
			x = newX;
			y = newY;
			placeAgent(state);
		}
	}

	public int decideX(Environment state, Bag neighbors) {
		//count neighbors to the right (posX) and left (negX) of this agent
		int posX = 0;
		int negX = 0;
		for(int i = 0; i < neighbors.numObjs; i++) {
			Agent a = (Agent)neighbors.objs[i];
			if(a.x > x) posX++;
			else if(a.x < x) negX++;
		}
		if(posX > negX) return 1;
		else if(negX > posX) return -1;
		else return 0;
	}

	public int decideY(Environment state, Bag neighbors) {
		//count neighbors above (posY) and below (negY)
		int posY = 0;
		int negY = 0;
		for(int i = 0; i < neighbors.numObjs; i++) {
			Agent a = (Agent)neighbors.objs[i];
			if(a.y > y) posY++;
			else if(a.y < y) negY++;
		}
		if(posY > negY) return 1;
		else if(negY > posY) return -1;
		else return 0;
	}

	public void aggregate (Environment state) {
		//get neighbors within searchRadius, excluding self
		Bag neighbors = state.sparseSpace.getMooreNeighbors(x, y, state.searchRadius,
				state.sparseSpace.TOROIDAL, false);
		//calculate direction toward the majority of neighbors
		dirx = decideX(state, neighbors);
		diry = decideY(state, neighbors);
		int newX = (x + dirx + state.gridWidth)  % state.gridWidth;
		int newY = (y + diry + state.gridHeight) % state.gridHeight;

		if(state.oneCellPerAgent) {
			Bag b = state.sparseSpace.getObjectsAtLocation(newX, newY);
			if(b == null || b.numObjs == 0) {
				state.sparseSpace.remove(this);
				x = newX;
				y = newY;
				placeAgent(state);
			}
		} else {
			state.sparseSpace.remove(this);
			x = newX;
			y = newY;
			placeAgent(state);
		}
	}

	public void step(SimState state) {
		Environment environment = (Environment)state;
		//with probability active, either aggregate or move randomly
		if(environment.random.nextBoolean(environment.active)) {
			if(environment.random.nextBoolean(environment.aggregate)) {
				aggregate(environment);
			} else {
				move(environment);
			}
		}

		if(!dated)
			date(environment);

	}

}