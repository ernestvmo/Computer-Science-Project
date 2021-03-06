package Optimisation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import WindowShading.WindowShadingFitnessFunction;
import main.Loader;
import plotting.PredictedPlotting;
import plotting.FacadeUI;
import plotting.Plotting;
import regression.Model;

/**
 * Class for the Non-dominated Sorting Genetic Algorithm.
 * 
 * @author Ernest Vanmosuinck
 */
public class NSGA2_E
{
	private FitnessFunction ff;
	/** Random object. */
	private Random r;

	/** The number of windows on a building's façade. */
	private int windowsCount = 120;
	/** Number of solutions per evaluations. */
	private int numSolutions = 100;
	/** Number of threads running at the same time. */
	private int numThreads = 10;
	/** Total number of evaluations. */
	private int maxEvals = 5000;

	// ************* NSGA-2 options *************
	/** Selection rate. */
	private double selectionRate = 0.5;
	/** Crossover rate. */
	private double crossoverRate = 0.5;
	/** Mutation rate. */
	private double mutationRate = 0.25; // 0 = low, 1 = high
	
	/** Surrogate model object. */
	private Model model;

	/**
	 * Constructor object for the NSGA.
	 */
	public NSGA2_E()
	{
		ff = new WindowShadingFitnessFunction(false, true);
		r = new Random();
	}

	/**
	 * Method to start the optimization algorithm.
	 */
	public void go()
	{
		System.out.println("started NSGA-II");

		VisualisePopulation vp = new VisualisePopulation();
		
		// 1 - initialize random population
		Individual[] initial = new Individual[numSolutions];
		initial[0] = new Individual(new boolean[windowsCount]);
		for (int i = 1; i < initial.length; i++)
		{
			initial[i] = new Individual(windowsCount, r);
		}
		
		evaluatePopulation(initial, false);
		initial = ascendList(nonDominatedSort(initial));
		
		// 2 - offspring
		Individual[] offspring = createOffspring(initial);
		evaluatePopulation(offspring, false);

		// System.out.println("TEST OFFSPRING " + offspring[0].getFitness1() + "
		// " + offspring[0].getFitness2());

		// for (Individual i : initial)
		// {
		// System.out.println(i.getFitness1() + " " + i.getFitness2());
		// }

		double firstPopulationHypervolume = Plotting.hypervolume(initial);

		int currentEval = 0;
		while (currentEval < maxEvals)
		{
			Individual[] R = new Individual[initial.length + offspring.length];
			int p = 0;
			for (int i = 0; i < initial.length; i++)
			{
				R[p++] = initial[i];
			}
			for (int i = 0; i < offspring.length; i++)
			{
				R[p++] = offspring[i];
			}

			List<List<Individual>> fronts = nonDominatedSort(R);

			initial = new Individual[numSolutions];
			int pointer = 0;
			List<Individual> nextFront = fronts.remove(0);
			while (pointer + nextFront.size() <= initial.length)
			{
				for (int i = 0; i < nextFront.size(); i++)
					initial[pointer++] = nextFront.get(i);
				nextFront = fronts.remove(0);
			}

			if (pointer < initial.length)
			{
				crowdingDistance(nextFront);
				Individual[] nextFrontArray = nextFront
						.toArray(new Individual[nextFront.size()]);

				Arrays.sort(nextFrontArray,
						new Individual.CrowdingDistanceComparator());

				for (int i = 0; pointer < initial.length; i++)
				{
					initial[pointer] = nextFrontArray[i];
					pointer++;
				}
			}

			offspring = createOffspring(initial);
			evaluatePopulation(offspring, false);


//			if (currentEval % 1000 == 0 && !(currentEval == maxEvals))
			if (currentEval % 100 == 0 && currentEval != 5000)
			{
				model.go();
			}
				
//			if (currentEval == (maxEvals / 2))
//			{
//				System.out.println("MID-PROCESS EVALUATION");
//				evaluatePopulation(initial, true);
//				
//				double[][] population = new double[initial.length][initial[0].getAlleles().length + 1];
//				System.out.println(population.length + " " + population[0].length);
//				for (int i = 0; i < population.length; i++)
//				{
//					for (int j = 0; j < initial[i].getAlleles().length; j++)
//					{
//						population[i][j] = initial[i].getAlleles()[j] ? 1 : 0;
//					}
//					population[i][initial.length] = initial[i].getFitness1();
//				}
//				
//				model.retrain(population);
//			}
			
			currentEval++;
			// System.out.println("eval: " + currentEval);
		}

		System.out.println("DONE");
		vp.updatePopulation(initial);
		// displayPopulation(initial);

//		for (Individual i : initial)
//		{
//			System.out.println(i.getFitness1() + " " + i.getFitness2());
//		}

		double lastPopulationHypervolume = Plotting.hypervolume(initial);
		
		// TODO : boxplot
		
		System.out.println("First: " + firstPopulationHypervolume);
		System.out.println("Last:  " + lastPopulationHypervolume);
		System.out.println("Improvement: " + (lastPopulationHypervolume - firstPopulationHypervolume));
		
		for (Individual i : initial)
			if (i.rank == 0)
				new FacadeUI(i);
		
		double mae = calculateMAE(initial);
		
		
		
		System.out.println("MAE " + mae);
		
//		System.out.println("Surrogate");
//		for (Individual i : surrogate)
//			System.out.println(i.toString());
//		evaluatePopulation(initial, true);
//		System.out.println("EnergyPlus");
//		for (Individual i : initial)
//			System.out.println(i.toString());

		
		// Boxplot
//		if (true) {
//			boxplot(initial);
//		}
	}

	private double calculateMAE(Individual[] initial)
	{
		double[] diff1 = new double[initial.length];
		for (int i = 0; i < initial.length; i++)
			diff1[i] = initial[i].getFitness1();
		
		evaluatePopulation(initial, true);

		double[] diff2 = new double[initial.length];
		for (int i = 0; i < initial.length; i++)
			diff2[i] = initial[i].getFitness1();
		
		double mae = 0;
		for (int i = 0; i < initial.length; i++)
			mae += (diff2[i] - diff1[i]);
		
		mae /= initial.length;
		
		return mae;
	}
	
	private void boxplot(Individual[] initial) {
		Individual[] B = new Individual[initial.length];
		for (int i = 0; i < B.length; i++)
			B[i] = new Individual(ff, initial[i].getAlleles());
		evaluatePopulation(B, true);
		double[] calculatedEnergy = new double[initial.length];
		double[] predictedEnergy = new double[initial.length];
		for (int i = 0; i < initial.length; i++) {
			calculatedEnergy[i] = B[i].getFitness1();
			predictedEnergy[i] = initial[i].getFitness1(); 
		}

		for (double val : predictedEnergy)
			System.out.println(val);
		for (double val : calculatedEnergy)
			System.out.println(val);
		
		new PredictedPlotting(calculatedEnergy, predictedEnergy);
	}

	/**
	 * This method evaluates the passed array of Individuals.
	 * Uses Threads to evaluate a population faster.
	 * 
	 * @param P The population to evaluate.
	 * @param energyplus a boolean value that determines if the evaluator need to use EnergyPlus. {@code true} if the evaluator uses EnergyPlus, {@code false} otherwise.
	 */
	private void evaluatePopulation(Individual[] P, boolean energyplus)
	{
		EvaluationThread[] evals = new EvaluationThread[numThreads];
		int numberPerThreads = numSolutions / numThreads;

		if (!energyplus)
			for (int i = 0; i < numThreads; i++)
			{
				evals[i] = new EvaluationThread(P, numberPerThreads * i,
						((i < numThreads - 1)
								? (numberPerThreads * (i + 1))
								: P.length));
				evals[i].start();
			}
		else
			for (int i = 0; i < numThreads; i++)
			{
				evals[i] = new EvaluationThread(P, numberPerThreads * i,
						((i < numThreads - 1)
								? (numberPerThreads * (i + 1))
								: P.length), true);
				evals[i].start();
			}

		for (int i = 0; i < numThreads; i++)
		{
			try
			{
				evals[i].join();
			}
			catch (InterruptedException e)
			{
				if (evals[i].isAlive())
					i--;
			}
		}
	}

	/**
	 * This method turns the passed front into an array of Individuals.
	 * 
	 * @param fronts The front of individuals.
	 * 
	 * @return An array of Individual objects.
	 */
	private Individual[] ascendList(List<List<Individual>> fronts)
	{
		List<Individual> P = new ArrayList<>();

		for (List<Individual> front : fronts)
			for (Individual i : front)
				P.add(i);

		return P.toArray(new Individual[P.size()]);
	}

	/**
	 * This method sorts Individuals in order of their domination rank over each other using the non-dominated sort.
	 * Individuals non dominated by others will be attributed the rank 0, the next ones rank 1, etc.
	 * 
	 * @param pop The population to sort.
	 * @return A list of list of fronts.
	 */
	private List<List<Individual>> nonDominatedSort(Individual[] pop)
	{
		// Initialize
		Set<Individual> assigned = new HashSet<Individual>(pop.length);
		
		// Keep track of what's been already assigned to a front
		List<List<Individual>> fronts = new ArrayList<List<Individual>>();
		fronts.add(new ArrayList<Individual>());// init first front

		// ====== block A =========
		// loop over all individuals in population and fill dom counts / sets
		for (int _this = 0; _this < pop.length; _this++)
		{
			pop[_this].dominationCount = 0;
			pop[_this].dominatedSet = new HashSet<Individual>();

			for (int _that = 0; _that < pop.length; _that++)
			{
				if (_this != _that)
				{ // don't compare with self
					if (pop[_this].dominates(pop[_that]))
					{ // if _this dominates _that
						pop[_this].dominatedSet.add(pop[_that]); // add _that to the set of solutions dominated by _this
					} 
					else if (pop[_that].dominates(pop[_this]))
					{
						pop[_this].dominationCount++; // increment the domination counter of _this
					}
				}
			}

			if (pop[_this].dominationCount == 0)
			{ // _this belongs to the first front
				pop[_this].rank = 0; 
				fronts.get(0).add(pop[_this]);
				assigned.add(pop[_this]);
			}
		}

		// =========== block B ==========
		int frontCounter = 0; // initialise the front counter; using base 0 not 1
		for (List<Individual> fi = fronts.get(frontCounter); fi.size() > 0; fi = fronts.get(frontCounter))
		{ 
			List<Individual> Q = new ArrayList<Individual>(); // Used to store members of the next front
			for (Individual p : fi)
			{
				for (Individual q : p.dominatedSet)
				{
					q.dominationCount--;
					if (q.dominationCount == 0)
					{
						q.rank = frontCounter + 1;
						Q.add(q);
					}
				}
			}
			frontCounter++;
			fronts.add(frontCounter, Q);
		}

		return fronts;
	}

	/**
	 * This method calculates the crowding distance for the List of fronts.
	 * 
	 * @param individuals The front to calculate the distance.
	 */
	private void crowdingDistance(List<Individual> individuals)
	{
		Individual[] I = individuals
				.toArray(new Individual[individuals.size()]);

		int l = I.length;

		for (Individual i : I)
		{
			i.distance = 0;
		}

		Arrays.sort(I, new Individual.Objective1Comparator());

		I[0].distance = Double.POSITIVE_INFINITY;
		I[l - 1].distance = Double.POSITIVE_INFINITY;

		double fitness1Range = I[l - 1].getFitness1() - I[0].getFitness1();
		for (int j = 1; j < l - 1; j++)
		{
			I[j].distance += (I[j + 1].getFitness1() - I[j - 1].getFitness1())
					/ fitness1Range;
		}

		Arrays.sort(I, new Individual.Objective2Comparator());

		I[0].distance = Double.POSITIVE_INFINITY;
		I[l - 1].distance = Double.POSITIVE_INFINITY;

		double fitness2Range = I[l - 1].getFitness2() - I[0].getFitness2();
		for (int j = 1; j < l - 1; j++)
		{
			I[j].distance += (I[j + 1].getFitness2() - I[j - 1].getFitness2())
					/ fitness2Range;
		}

	}

	/**
	 * Creates an offspring population from the passed population.
	 * 
	 * @param parents The parent population to create the offspring population from.
	 * @return The offspring population.
	 */
	private Individual[] createOffspring(Individual[] parents)
	{
		Individual[] nextPopulation = new Individual[parents.length];

		for (int i = 0; i < nextPopulation.length; i += 2)
		{// replace half the population
			// Parent 1
			Individual parent1 = parentSelection(parents);
			// Parent 2
			Individual parent2 = parentSelection(parents);

			// Crossover
			// TODO: play with different crossover strategies?
			Individual[] offspring = crossover(parent1, parent2);

			// Mutation
			offspring[0] = mutateOffspring(offspring[0]);
			offspring[1] = mutateOffspring(offspring[1]);

			nextPopulation[i] = offspring[0];
			if (i < nextPopulation.length - 1)
				nextPopulation[i + 1] = offspring[1];
		}

		return nextPopulation;
	}

	/**
	 * Selects the parents for creating the offspring individual based on the selection rate.
	 * 
	 * @param P The parent population.
	 * @return A random individual from the population.
	 */
	private Individual parentSelection(Individual[] P)
	{
		int i = r.nextInt(P.length), j = r.nextInt(P.length);

		while (i == j)
			j = r.nextInt(P.length);

		Individual p1 = P[i];
		Individual p2 = P[j];

		if (r.nextDouble() < selectionRate)
		{
			if (p1.rank <= p2.rank)
				return p1;
			else
				return p2;
		} else
		{
			if (p1.rank <= p2.rank)
				return p2;
			else
				return p1;
		}
	}

	/**
	 * Picks which allele is carried over from the parent based on the crossover rate.
	 * 
	 * @param parent1 The first parent.
	 * @param parent2 The second parent.
	 * @return The offspring population.
	 */
	private Individual[] crossover(Individual parent1, Individual parent2)
	{
		boolean[] alleles1 = parent1.getAlleles();
		boolean[] alleles2 = parent2.getAlleles();

		boolean[] next1 = new boolean[alleles1.length];
		boolean[] next2 = new boolean[alleles1.length];

		// crossover
		for (int b = 0; b < next1.length; b++)
		{
			if (r.nextDouble() < crossoverRate)
				next1[b] = alleles1[b];
			else
				next1[b] = alleles2[b];
		}
		for (int b = 0; b < next2.length; b++)
		{
			if (r.nextDouble() < crossoverRate)
				next2[b] = alleles1[b];
			else
				next2[b] = alleles2[b];
		}

		Individual[] generatedOffspring = new Individual[2];
		generatedOffspring[0] = new Individual(next1);
		generatedOffspring[1] = new Individual(next2);

		return generatedOffspring;
	}

	/**
	 * Mutates the individual based on the mutation rate.
	 * 
	 * @param offspring The offspring individual.
	 * @return The possibly mutated offspring individual.
	 */
	private Individual mutateOffspring(Individual offspring)
	{
		boolean[] alleles = offspring.getAlleles();

		boolean[] mutated = new boolean[alleles.length];

		for (int i = 0; i < mutated.length; i++)
		{
			if (r.nextDouble() < mutationRate)
				mutated[i] = !alleles[i];
			else
				mutated[i] = alleles[i];
		}

		return new Individual(mutated);
	}

	/**
	 * EvaluationThread class that will evaluate a population.
	 * 
	 * @author Ernest Vanmosuinck
	 */
	class EvaluationThread extends Thread
	{
		/** The population to evaluate. */
		private Individual[] individuals;
		/** The index in the array to start evaluating from. */
		private int startIndex;
		/** The index in the array to start evaluating. */
		private int endIndex;
		/** Whether or not the evaluation has to use EnergyPlus. */
		private boolean energyplus;

		/**
		 * Constructor for the EvaluationThread object. 
		 * Uses the surrogate model to evaluate fitness.
		 * 
		 * @param individuals The population to evaluate.
		 * @param startIndex The index to start evaluating from.
		 * @param endIndex The index to stop evaluating from.
		 */
		public EvaluationThread(Individual[] individuals, int startIndex, int endIndex)
		{
			this.individuals = individuals;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.energyplus = false;
		}
		
		/**
		 * Constructor for the EvaluationThread object. 
		 * Uses EnergyPlus to evaluate fitness.
		 * 
		 * @param individuals The population to evaluate.
		 * @param startIndex The index to start evaluating from.
		 * @param endIndex The index to stop evaluating from.
		 */
		public EvaluationThread(Individual[] individuals, int startIndex, int endIndex, boolean energyplus)
		{
			this.individuals = individuals;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.energyplus = true;
		}

		public void run()
		{
			for (int j = startIndex; j < endIndex; j++)
			{
				Individual i = individuals[j];
				if (!energyplus)
					i.surrogateEvaluate(model); // TODO
				else
					i.energyPlusEvaluate(ff);
				// System.out.println("SINGLE INDIVIDUAL : " + i.getFitness1() +
				// " " + i.getFitness2());
			}
		}
	}

	/**
	 * Mutator method for the surrogate model object.
	 * 
	 * @param m The surrogate model.
	 */
	public void setModel(Model m)
	{
		this.model = m;
	}

	private void displayPopulation(Individual[] P)
	{
		for (Individual i : P)
		{
			for (int j = 0; j < i.getAlleles().length; j++)
			{
				System.out.print(i.getAlleles()[j] ? "1" : "0");
			}
			System.out.println();
		}
	}
}
