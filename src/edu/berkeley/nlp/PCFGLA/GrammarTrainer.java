package edu.berkeley.nlp.PCFGLA;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.berkeley.nlp.PCFGLA.Corpus.TreeBankType;
import edu.berkeley.nlp.PCFGLA.smoothing.NoSmoothing;
import edu.berkeley.nlp.PCFGLA.smoothing.SmoothAcrossParentBits;
import edu.berkeley.nlp.PCFGLA.smoothing.Smoother;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;

/**
 * Reads in the Penn Treebank and generates N_GRAMMARS different grammars.
 * 
 * @author Slav Petrov
 */
public class GrammarTrainer {

	public static boolean VERBOSE = false;
	public static int HORIZONTAL_MARKOVIZATION = 1;
	public static int VERTICAL_MARKOVIZATION = 2;
	public static Random RANDOM = new Random(0);

	public static class Options {

		@Option(name = "-out", required = true, usage = "Output File for Grammar (Required)")
		public String outFileName;

		@Option(name = "-path", usage = "Path to Corpus (Default: null)")
		public String path = null;

		@Option(name = "-SMcycles", usage = "The number of split&merge iterations (Default: 6)")
		public int numSplits = 6;

		@Option(name = "-mergingPercentage", usage = "Merging percentage (Default: 0.5)")
		public double mergingPercentage = 0.5;

		@Option(name = "-baseline", usage = "Just read of the MLE baseline grammar")
		public boolean baseline = false;

		@Option(name = "-treebank", usage = "Language:  WSJ, CHNINESE, GERMAN, CONLL, SINGLEFILE (Default: ENGLISH)")
		public TreeBankType treebank = TreeBankType.WSJ;

		@Option(name = "-splitMaxIt", usage = "Maximum number of EM iterations after splitting (Default: 50)")
		public int splitMaxIterations = 50;

		@Option(name = "-splitMinIt", usage = "Minimum number of EM iterations after splitting (Default: 50)")
		public int splitMinIterations = 50;

		@Option(name = "-mergeMaxIt", usage = "Maximum number of EM iterations after merging (Default: 20)")
		public int mergeMaxIterations = 20;

		@Option(name = "-mergeMinIt", usage = "Minimum number of EM iterations after merging (Default: 20)")
		public int mergeMinIterations = 20;

		@Option(name = "-smoothMaxIt", usage = "Maximum number of EM iterations with smoothing (Default: 10)")
		public int smoothMaxIterations = 10;

		@Option(name = "-di", usage = "The number of allowed iterations in which the validation likelihood drops. (Default: 6)")
		public int di = 6;

		@Option(name = "-trfr", usage = "The fraction of the training corpus to keep (Default: 1.0)\n")
		public double trainingFractionToKeep = 1.0;

		@Option(name = "-filter", usage = "Filter rules with prob below this threshold (Default: 1.0e-30)")
		public double filter = 1.0e-30;

		@Option(name = "-smooth", usage = "Type of grammar smoothing used.")
		public String smooth = "SmoothAcrossParentBits";

		@Option(name = "-maxL", usage = "Maximum sentence length (Default <=10000)")
		public int maxSentenceLength = 10000;

		@Option(name = "-b", usage = "LEFT/RIGHT Binarization (Default: RIGHT)")
		public Binarization binarization = Binarization.RIGHT;

		@Option(name = "-noSplit", usage = "Don't split - just load and continue training an existing grammar (true/false) (Default:false)")
		public boolean noSplit = false;

		@Option(name = "-in", usage = "Input File for Grammar")
		public String inFile = null;

		@Option(name = "-randSeed", usage = "Seed for random number generator (Two works well for English)")
		public int randSeed = 2;

		@Option(name = "-sep", usage = "Set merging threshold for grammar and lexicon separately (Default: false)")
		public boolean separateMergingThreshold = false;

		@Option(name = "-trainOnDevSet", usage = "Include the development set into the training set (Default: false)")
		public boolean trainOnDevSet = false;

		@Option(name = "-hor", usage = "Horizontal Markovization (Default: 0)")
		public int horizontalMarkovization = 0;

		@Option(name = "-sub", usage = "Number of substates to split (Default: 2)")
		public short nSubStates = 1;

		@Option(name = "-ver", usage = "Vertical Markovization (Default: 1)")
		public int verticalMarkovization = 1;

		@Option(name = "-v", usage = "Verbose/Quiet (Default: Quiet)\n")
		public boolean verbose = false;

		@Option(name = "-lowercase", usage = "Lowercase all words in the treebank")
		public boolean lowercase = false;

		@Option(name = "-r", usage = "Level of Randomness at init (Default: 1)\n")
		public double randomization = 1.0;

		@Option(name = "-sm1", usage = "Lexicon smoothing parameter 1")
		public double smoothingParameter1 = 0.5;

		@Option(name = "-sm2", usage = "Lexicon smoothing parameter 2")
		public double smoothingParameter2 = 0.1;

		@Option(name = "-rare", usage = "Rare word threshold (Default 20)")
		public int rare = 20;

		@Option(name = "-reallyRare", usage = "Really Rare word threshold (Default 10)")
		public int reallyRare = 10;

		@Option(name = "-spath", usage = "Whether or not to store the best path info (true/false) (Default: true)")
		public boolean findClosedUnaryPaths = true;

		@Option(name = "-simpleLexicon", usage = "Use the simple generative lexicon")
		public boolean simpleLexicon = false;

		@Option(name = "-featurizedLexicon", usage = "Use the featurized lexicon")
		public boolean featurizedLexicon = false;

		@Option(name = "-skipSection", usage = "Skips a particular section of the WSJ training corpus (Needed for training Mark Johnsons reranker")
		public int skipSection = -1;

		@Option(name = "-skipBilingual", usage = "Skips the bilingual portion of the Chinese treebank (Needed for training the bilingual reranker")
		public boolean skipBilingual = false;

		@Option(name = "-keepFunctionLabels", usage = "Retain predicted function labels. Model must have been trained with function labels. (Default: false)")
		public boolean keepFunctionLabels = false;
	}
	
	protected static Options parseOptions(String[] args) {
		OptionParser optParser = new OptionParser(Options.class);
		// provide feedback on command-line arguments
		Options opts = (Options) optParser.parse(args, true); 
		System.out.println("Calling with " + optParser.getPassedInOptions());

		System.out.println("Loading trees from " + opts.path
				+ " and using language " + opts.treebank);

		System.out.println("Will remove sentences with more than "
				+ opts.maxSentenceLength + " words.");

		System.out.println("Using horizontal=" + opts.horizontalMarkovization
				+ " and vertical=" + opts.verticalMarkovization
				+ " markovization.");

		HORIZONTAL_MARKOVIZATION = opts.horizontalMarkovization;
		VERTICAL_MARKOVIZATION = opts.verticalMarkovization;
		
		System.out.println("Using " + opts.binarization.name() + " binarization.");

		System.out.println("Using a randomness value of " + opts.randomization);

		VERBOSE = opts.verbose;
		RANDOM = new Random(opts.randSeed);
		System.out.println("Random number generator seeded at " + opts.randSeed
				+ ".");

		if (opts.outFileName == null) {
			System.out.println("Output File name is required.");
			System.exit(-1);
		} else
			System.out
					.println("Using grammar output file " + opts.outFileName + ".");

		if (opts.baseline)
			opts.numSplits = 0;

		if (opts.filter > 0)
			System.out.println("Will remove rules with prob under "
				+ opts.filter
				+ ".\nEven though only unlikely rules are pruned the training LL is not guaranteed to increase in every round anymore "
				+ "(especially when we are close to converging)."
				+ "\nFurthermore it increases the variance because 'good' rules can be pruned away in early stages.");
		
		return opts;
	}
	
	public static short[] createSubStatesArray(
			List<Tree<String>> trainTrees, 
			List<Tree<String>> validationTrees, 
			Numberer tagNumberer, 
			Options opts
		) {
		short [] numSubStatesArray = initializeSubStateArray(
			trainTrees,
			validationTrees, 
			tagNumberer, 
			opts.nSubStates
		);
		if (opts.baseline) {
			Arrays.fill(numSubStatesArray, (short) 1);
			System.out
					.println("Training just the baseline grammar (1 substate for all states)");
			opts.randomization = 0.0f;
		}

		if (VERBOSE) {
			for (int i = 0; i < numSubStatesArray.length; i++) {
				System.out.println("Tag " + (String) tagNumberer.object(i)
						+ " " + i);
			}
		}

		System.out.println("There are " + numSubStatesArray.length
				+ " observed categories.");

		return numSubStatesArray;
	}
	
	protected static Lexicon getLexicon(
		Options opts,
		short [] numSubStatesArray,
		double [] smoothParams,
		Featurizer feat,
		StateSetTreeList trainStateSetTrees
	) {
		if (opts.simpleLexicon) 
			return new SimpleLexicon(
				numSubStatesArray, 
				-1, 
				smoothParams, 
				new NoSmoothing(),
				opts.filter, 
				trainStateSetTrees
			);
		else if (opts.featurizedLexicon)
			return new FeaturizedLexicon(numSubStatesArray, feat,
					trainStateSetTrees);
		else
			return new SophisticatedLexicon(
				numSubStatesArray,
				SophisticatedLexicon.DEFAULT_SMOOTHING_CUTOFF,
				smoothParams, 
				new NoSmoothing(), 
				opts.filter
			);
		
	}

	public static void main(String[] args) {
		Options opts = parseOptions(args);

		Corpus corpus = new Corpus(opts.path, opts.treebank, opts.trainingFractionToKeep,
				false, opts.skipSection, opts.skipBilingual,
				opts.keepFunctionLabels);

		List<Tree<String>> trainTrees = Corpus.binarizeAndFilterTrees(
				corpus.getTrainTrees(), VERTICAL_MARKOVIZATION,
				HORIZONTAL_MARKOVIZATION, opts.maxSentenceLength, opts.binarization,
				false, VERBOSE);
		List<Tree<String>> validationTrees = Corpus.binarizeAndFilterTrees(
				corpus.getValidationTrees(), VERTICAL_MARKOVIZATION,
				HORIZONTAL_MARKOVIZATION, opts.maxSentenceLength, opts.binarization,
				false, VERBOSE);

		if (opts.trainOnDevSet) {
			System.out.println("Adding devSet to training data.");
			trainTrees.addAll(validationTrees);
		}

		if (opts.lowercase) {
			System.out.println("Lowercasing the treebank.");
			Corpus.lowercaseWords(trainTrees);
			Corpus.lowercaseWords(validationTrees);
		}

		int nTrees = trainTrees.size();
		System.out.println("There are " + trainTrees.size()
				+ " trees in the training set.");

		Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

		short[] numSubStatesArray = createSubStatesArray(trainTrees, validationTrees, tagNumberer, opts);
		
		// EM: iterate until the validation likelihood drops for four
		// consecutive
		// iterations

		// If we are splitting, we load the old grammar and start off by
		// splitting.
		// initialize lexicon and grammar
		Lexicon lexicon = null, maxLexicon = null, previousLexicon = null;
		Grammar grammar = null, maxGrammar = null, previousGrammar = null;
		int startSplit = 0;
		if (opts.inFile != null) {
			System.out.println("Loading old grammar from " + opts.inFile);
			startSplit = 0; // we've already trained the grammar
			ParserData pData = ParserData.Load(opts.inFile);
			maxGrammar = pData.gr;
			maxLexicon = pData.lex;
			numSubStatesArray = maxGrammar.numSubStates;
			previousGrammar = grammar = maxGrammar;
			previousLexicon = lexicon = maxLexicon;
			Numberer.setNumberers(pData.getNumbs());
			tagNumberer = Numberer.getGlobalNumberer("tags");
			System.out.println("Loading old grammar complete.");
			if (opts.noSplit) {
				System.out.println("Will NOT split the loaded grammar.");
				startSplit = 1;
			}
		}

		StateSetTreeList trainStateSetTrees = new StateSetTreeList(
			trainTrees,
			numSubStatesArray, 
			false, 
			tagNumberer
		);
		StateSetTreeList validationStateSetTrees = new StateSetTreeList(
			validationTrees, 
			numSubStatesArray, 
			false, 
			tagNumberer
		);

		// get rid of the old trees
		trainTrees = null;
		validationTrees = null;
		corpus = null;
		System.gc();

		if (opts.simpleLexicon) {
			System.out
					.println("Replacing words which have been seen less than 5 times with their signature.");
			Corpus.replaceRareWords(trainStateSetTrees, new SimpleLexicon(
					numSubStatesArray, -1), opts.rare);
		}

		Featurizer feat = new SimpleFeaturizer(opts.rare, opts.reallyRare);

		double[] smoothParams = { 
			opts.smoothingParameter1,
			opts.smoothingParameter2 
		};
		System.out.println("Using smoothing parameters " + smoothParams[0]
				+ " and " + smoothParams[1]);

		// If we're training without loading a split grammar, then we run once
		// without splitting.
		if (opts.inFile == null) {
			grammar = new Grammar(
				numSubStatesArray, 
				opts.findClosedUnaryPaths,
				new NoSmoothing(), 
				null, 
				opts.filter
			);
			
			Lexicon tmp_lexicon = getLexicon(
				opts, 
				numSubStatesArray, 
				smoothParams, 
				feat, 
				trainStateSetTrees
			);
			
			int n = 0;
			boolean secondHalf = false;
			for (Tree<StateSet> stateSetTree : trainStateSetTrees) {
				secondHalf = (n++ > nTrees / 2.0);
				tmp_lexicon.trainTree(
					stateSetTree, 
					opts.randomization, 
					null,
					secondHalf, 
					false, 
					opts.rare
				);
			}
			lexicon = getLexicon(
				opts,
				numSubStatesArray,
				smoothParams,
				feat,
				trainStateSetTrees
			); 
			for (Tree<StateSet> stateSetTree : trainStateSetTrees) {
				secondHalf = (n++ > nTrees / 2.0);
				lexicon.trainTree(
					stateSetTree, 
					opts.randomization, 
					tmp_lexicon,
					secondHalf, 
					false, 
					opts.rare
				);
				grammar.tallyUninitializedStateSetTree(stateSetTree);
			}
			lexicon.tieRareWordStats(opts.rare);
			lexicon.optimize();
			grammar.optimize(opts.randomization);
			// System.out.println(grammar);
			previousGrammar = maxGrammar = grammar; // needed for baseline -
													// when there is no EM loop
			previousLexicon = maxLexicon = lexicon;
		}

		int maxIterations = opts.splitMaxIterations;
		int minIterations = opts.splitMinIterations;
		if (minIterations > 0)
			System.out.println("I will do at least " + minIterations
					+ " and at most" + maxIterations + " iterations.");

		boolean allowMoreSubstatesThanCounts = false;

		double maxLikelihood = Double.NEGATIVE_INFINITY;

		double mergingPercentage = opts.mergingPercentage;
		boolean separateMergingThreshold = opts.separateMergingThreshold;
		if (mergingPercentage > 0) {
			System.out.println("Will merge " + (int) (mergingPercentage * 100)
					+ "% of the splits in each round.");
			System.out
					.println("The threshold for merging lexical and phrasal categories will be set separately: "
							+ separateMergingThreshold);
		}
		int iter = 0;
		// the main loop: split and train the grammar
		for (int splitIndex = startSplit; splitIndex < opts.numSplits * 3; splitIndex++) {

			// now do either a merge or a split and the end a smooth
			// on odd iterations merge, on even iterations split
			String opString = "";
			if (splitIndex % 3 == 2) {// (splitIndex==numSplitTimes*2){
				if (opts.smooth.equals("NoSmoothing"))
					continue;
				System.out.println("Setting smoother for grammar and lexicon.");
				Smoother grSmoother = new SmoothAcrossParentBits(0.01,
						maxGrammar.splitTrees);
				Smoother lexSmoother = new SmoothAcrossParentBits(0.1,
						maxGrammar.splitTrees);
				// Smoother grSmoother = new SmoothAcrossParentSubstate(0.01);
				// Smoother lexSmoother = new SmoothAcrossParentSubstate(0.1);
				maxGrammar.setSmoother(grSmoother);
				maxLexicon.setSmoother(lexSmoother);
				minIterations = maxIterations = opts.smoothMaxIterations;
				opString = "smoothing";
			} else if (splitIndex % 3 == 0) {
				// the case where we split
				if (opts.noSplit)
					continue;
				System.out.println("Before splitting, we have a total of "
						+ maxGrammar.totalSubStates() + " substates.");
				CorpusStatistics corpusStatistics = new CorpusStatistics(
						tagNumberer, trainStateSetTrees);
				int[] counts = corpusStatistics.getSymbolCounts();

				maxGrammar = maxGrammar.splitAllStates(opts.randomization, counts,
						allowMoreSubstatesThanCounts, 0);
				maxLexicon = maxLexicon.splitAllStates(counts,
						allowMoreSubstatesThanCounts, 0);
				Smoother grSmoother = new NoSmoothing();
				Smoother lexSmoother = new NoSmoothing();
				maxGrammar.setSmoother(grSmoother);
				maxLexicon.setSmoother(lexSmoother);
				System.out.println("After splitting, we have a total of "
						+ maxGrammar.totalSubStates() + " substates.");
				System.out
						.println("Rule probabilities are NOT normalized in the split, therefore the training LL is not guaranteed to improve between iteration 0 and 1!");
				opString = "splitting";
				maxIterations = opts.splitMaxIterations;
				minIterations = opts.splitMinIterations;
			} else {
				if (mergingPercentage == 0)
					continue;
				// the case where we merge
				double[][] mergeWeights = GrammarMerger.computeMergeWeights(
						maxGrammar, maxLexicon, trainStateSetTrees);
				double[][][] deltas = GrammarMerger.computeDeltas(maxGrammar,
						maxLexicon, mergeWeights, trainStateSetTrees);
				boolean[][][] mergeThesePairs = GrammarMerger
						.determineMergePairs(deltas, separateMergingThreshold,
								mergingPercentage, maxGrammar);

				grammar = GrammarMerger.doTheMerges(maxGrammar, maxLexicon,
						mergeThesePairs, mergeWeights);
				short[] newNumSubStatesArray = grammar.numSubStates;
				trainStateSetTrees = new StateSetTreeList(trainStateSetTrees,
						newNumSubStatesArray, false);
				validationStateSetTrees = new StateSetTreeList(
						validationStateSetTrees, newNumSubStatesArray, false);

				// retrain lexicon to finish the lexicon merge (updates the
				// unknown words model)...
				if (opts.featurizedLexicon) {
					lexicon = new FeaturizedLexicon(newNumSubStatesArray, feat,
							trainStateSetTrees);
				} else
					lexicon = (opts.simpleLexicon) ? new SimpleLexicon(
							newNumSubStatesArray, -1, smoothParams,
							maxLexicon.getSmoother(), opts.filter,
							trainStateSetTrees) : new SophisticatedLexicon(
							newNumSubStatesArray,
							SophisticatedLexicon.DEFAULT_SMOOTHING_CUTOFF,
							maxLexicon.getSmoothingParams(),
							maxLexicon.getSmoother(),
							maxLexicon.getPruningThreshold());
				boolean updateOnlyLexicon = true;
				GrammarTrainer.doOneEStep(grammar,
						maxLexicon, null, lexicon, trainStateSetTrees,
						updateOnlyLexicon, opts.rare);
				// System.out.println("The training LL is "+trainingLikelihood);
				lexicon.optimize();// Grammar.RandomInitializationType.INITIALIZE_WITH_SMALL_RANDOMIZATION);
									// // M Step

				GrammarMerger.printMergingStatistics(maxGrammar, grammar);
				opString = "merging";
				maxGrammar = grammar;
				maxLexicon = lexicon;
				maxIterations = opts.mergeMaxIterations;
				minIterations = opts.mergeMinIterations;
			}
			// update the substate dependent objects
			previousGrammar = grammar = maxGrammar;
			previousLexicon = lexicon = maxLexicon;
			int droppingIter = 0;
			numSubStatesArray = grammar.numSubStates;
			trainStateSetTrees = new StateSetTreeList(trainStateSetTrees,
					numSubStatesArray, false);
			validationStateSetTrees = new StateSetTreeList(
					validationStateSetTrees, numSubStatesArray, false);
			maxLikelihood = calculateLogLikelihood(maxGrammar, maxLexicon,
					validationStateSetTrees);
			System.out.println("After " + opString + " in the "
					+ (splitIndex / 3 + 1)
					+ "th round, we get a validation likelihood of "
					+ maxLikelihood);
			iter = 0;

			// the inner loop: train the grammar via EM until validation
			// likelihood reliably drops
			do {
				iter += 1;
				System.out.println("Beginning iteration " + (iter - 1) + ":");

				// 1) Compute the validation likelihood of the previous
				// iteration
				System.out.print("Calculating validation likelihood...");
				double validationLikelihood = calculateLogLikelihood(
						previousGrammar, previousLexicon,
						validationStateSetTrees); // The validation LL of
													// previousGrammar/previousLexicon
				System.out.println("done: " + validationLikelihood);

				// 2) Perform the E step while computing the training likelihood
				// of the previous iteration
				System.out.print("Calculating training likelihood...");
				grammar = new Grammar(grammar.numSubStates,
						grammar.findClosedPaths, grammar.smoother, grammar,
						grammar.threshold);
				if (opts.featurizedLexicon)
					lexicon = lexicon.copyLexicon();
				// lexicon = new
				// FeaturizedLexicon(numSubStatesArray,feat,trainStateSetTrees);
				else
					lexicon = (opts.simpleLexicon) ? new SimpleLexicon(
							grammar.numSubStates, -1, smoothParams,
							lexicon.getSmoother(), opts.filter, trainStateSetTrees)
							: new SophisticatedLexicon(
									grammar.numSubStates,
									SophisticatedLexicon.DEFAULT_SMOOTHING_CUTOFF,
									lexicon.getSmoothingParams(), lexicon
											.getSmoother(), lexicon
											.getPruningThreshold());
				boolean updateOnlyLexicon = false;
				double trainingLikelihood = doOneEStep(previousGrammar,
						previousLexicon, grammar, lexicon, trainStateSetTrees,
						updateOnlyLexicon, opts.rare); // The training LL of
														// previousGrammar/previousLexicon
				System.out.println("done: " + trainingLikelihood);

				// 3) Perform the M-Step
				lexicon.optimize(); // M Step
				grammar.optimize(0); // M Step

				// 4) Check whether previousGrammar/previousLexicon was in fact
				// better than the best
				if (iter < minIterations
						|| validationLikelihood >= maxLikelihood) {
					maxLikelihood = validationLikelihood;
					maxGrammar = previousGrammar;
					maxLexicon = previousLexicon;
					droppingIter = 0;
				} else {
					droppingIter++;
				}

				// 5) advance the 'pointers'
				previousGrammar = grammar;
				previousLexicon = lexicon;
			} while ((droppingIter < opts.di) && (!opts.baseline)
					&& (iter < maxIterations));

			// Dump a grammar file to disk from time to time
			ParserData pData = new ParserData(maxLexicon, maxGrammar, null,
					Numberer.getNumberers(), numSubStatesArray,
					VERTICAL_MARKOVIZATION, HORIZONTAL_MARKOVIZATION,
					opts.binarization);
			String outTmpName = opts.outFileName + "_" + (splitIndex / 3 + 1) + "_"
					+ opString + ".gr";
			System.out.println("Saving grammar to " + outTmpName + ".");
			if (pData.Save(outTmpName))
				System.out.println("Saving successful.");
			else
				System.out.println("Saving failed!");
			pData = null;

		}

		// The last grammar/lexicon has not yet been evaluated. Even though the
		// validation likelihood
		// has been dropping in the past few iteration, there is still a chance
		// that the last one was in
		// fact the best so just in case we evaluate it.
		System.out.print("Calculating last validation likelihood...");
		double validationLikelihood = calculateLogLikelihood(grammar, lexicon,
				validationStateSetTrees);
		System.out.println("done.\n  Iteration " + iter
				+ " (final) gives validation likelihood "
				+ validationLikelihood);
		if (validationLikelihood > maxLikelihood) {
			maxLikelihood = validationLikelihood;
			maxGrammar = previousGrammar;
			maxLexicon = previousLexicon;
		}

		ParserData pData = new ParserData(maxLexicon, maxGrammar, null,
				Numberer.getNumberers(), numSubStatesArray,
				VERTICAL_MARKOVIZATION, HORIZONTAL_MARKOVIZATION, opts.binarization);

		System.out.println("Saving grammar to " + opts.outFileName + ".");
		System.out.println("It gives a validation data log likelihood of: "
				+ maxLikelihood);
		if (pData.Save(opts.outFileName))
			System.out.println("Saving successful.");
		else
			System.out.println("Saving failed!");

		System.exit(0);
	}

	/**
	 * @param previousGrammar
	 * @param previousLexicon
	 * @param grammar
	 * @param lexicon
	 * @param trainStateSetTrees
	 * @return
	 */
	public static double doOneEStep(Grammar previousGrammar,
			Lexicon previousLexicon, Grammar grammar, Lexicon lexicon,
			StateSetTreeList trainStateSetTrees, boolean updateOnlyLexicon,
			int unkThreshold) {
		boolean secondHalf = false;
		ArrayParser parser = new ArrayParser(previousGrammar, previousLexicon);
		double trainingLikelihood = 0;
		int n = 0;
		int nTrees = trainStateSetTrees.size();
		for (Tree<StateSet> stateSetTree : trainStateSetTrees) {
			secondHalf = (n++ > nTrees / 2.0);
			boolean noSmoothing = true, debugOutput = false;
			parser.doInsideOutsideScores(stateSetTree, noSmoothing, debugOutput); // E
																					// Step
			double ll = stateSetTree.getLabel().getIScore(0);
			ll = Math.log(ll) + (100 * stateSetTree.getLabel().getIScale());// System.out.println(stateSetTree);
			if ((Double.isInfinite(ll) || Double.isNaN(ll))) {
				if (VERBOSE) {
					System.out.println("Training sentence " + n + " is given "
							+ ll + " log likelihood!");
					System.out.println("Root iScore "
							+ stateSetTree.getLabel().getIScore(0) + " scale "
							+ stateSetTree.getLabel().getIScale());
				}
			} else {
				lexicon.trainTree(stateSetTree, -1, previousLexicon,
						secondHalf, noSmoothing, unkThreshold);
				if (!updateOnlyLexicon)
					grammar.tallyStateSetTree(stateSetTree, previousGrammar); // E
																				// Step
				trainingLikelihood += ll; // there are for some reason some
											// sentences that are unparsable
			}
		}
		lexicon.tieRareWordStats(unkThreshold);
		return trainingLikelihood;
	}

	/**
	 * @param maxGrammar
	 * @param maxLexicon
	 * @param validationStateSetTrees
	 * @return
	 */
	public static double calculateLogLikelihood(Grammar maxGrammar,
			Lexicon maxLexicon, StateSetTreeList validationStateSetTrees) {
		ArrayParser parser = new ArrayParser(maxGrammar, maxLexicon);
		int unparsable = 0;
		double maxLikelihood = 0;
		for (Tree<StateSet> stateSetTree : validationStateSetTrees) {
			parser.doInsideScores(stateSetTree, false, false, null); // Only
																		// inside
																		// scores
																		// are
																		// needed
																		// here
			double ll = stateSetTree.getLabel().getIScore(0);
			ll = Math.log(ll) + (100 * stateSetTree.getLabel().getIScale());
			if (Double.isInfinite(ll) || Double.isNaN(ll)) {
				unparsable++;
				// printBadLLReason(stateSetTree, lexicon);
			} else
				maxLikelihood += ll; // there are for some reason some sentences
										// that are unparsable
		}
		// if (unparsable>0)
		// System.out.print("Number of unparsable trees: "+unparsable+".");
		return maxLikelihood;
	}

	/**
	 * @param stateSetTree
	 */
	public static void printBadLLReason(Tree<StateSet> stateSetTree,
			SophisticatedLexicon lexicon) {
		System.out.println(stateSetTree.toString());
		boolean lexiconProblem = false;
		List<StateSet> words = stateSetTree.getYield();
		Iterator<StateSet> wordIterator = words.iterator();
		for (StateSet stateSet : stateSetTree.getPreTerminalYield()) {
			String word = wordIterator.next().getWord();
			boolean lexiconProblemHere = true;
			for (int i = 0; i < stateSet.numSubStates(); i++) {
				double score = stateSet.getIScore(i);
				if (!(Double.isInfinite(score) || Double.isNaN(score))) {
					lexiconProblemHere = false;
				}
			}
			if (lexiconProblemHere) {
				System.out.println("LEXICON PROBLEM ON STATE "
						+ stateSet.getState() + " word " + word);
				System.out.println("  word "
						+ lexicon.wordCounter.getCount(stateSet.getWord()));
				for (int i = 0; i < stateSet.numSubStates(); i++) {
					System.out.println("  tag "
							+ lexicon.tagCounter[stateSet.getState()][i]);
					System.out.println("  word/state/sub "
							+ lexicon.wordToTagCounters[stateSet.getState()]
									.get(stateSet.getWord())[i]);
				}
			}
			lexiconProblem = lexiconProblem || lexiconProblemHere;
		}
		if (lexiconProblem)
			System.out
					.println("  the likelihood is bad because of the lexicon");
		else
			System.out
					.println("  the likelihood is bad because of the grammar");
	}

	/**
	 * This function probably doesn't belong here, but because it should be
	 * called after {@link #updateStateSetTrees}, Leon left it here.
	 * 
	 * @param trees
	 *            Trees which have already had their inside-outside
	 *            probabilities calculated, as by {@link #updateStateSetTrees}.
	 * @return The log likelihood of the trees.
	 */
	public static double logLikelihood(List<Tree<StateSet>> trees,
			boolean verbose) {
		double likelihood = 0, l = 0;
		for (Tree<StateSet> tree : trees) {
			l = tree.getLabel().getIScore(0);
			if (verbose)
				System.out.println("LL is " + l + ".");
			if (Double.isInfinite(l) || Double.isNaN(l)) {
				System.out.println("LL is not finite.");
			} else {
				likelihood += l;
			}
		}
		return likelihood;
	}

	/**
	 * This updates the inside-outside probabilities for the list of trees using
	 * the parser's doInsideScores and doOutsideScores methods.
	 * 
	 * @param trees
	 *            A list of binarized, annotated StateSet Trees.
	 * @param parser
	 *            The parser to score the trees.
	 */
	public static void updateStateSetTrees(List<Tree<StateSet>> trees,
			ArrayParser parser) {
		for (Tree<StateSet> tree : trees) {
			parser.doInsideOutsideScores(tree, false, false);
		}
	}

	public static short[] initializeSubStateArray(
			List<Tree<String>> trainTrees, List<Tree<String>> validationTrees,
			Numberer tagNumberer, short nSubStates) {
		// first generate unsplit grammar and lexicon
		short[] nSub = new short[2];
		nSub[0] = 1;
		nSub[1] = nSubStates;

		// do the validation set so that the numberer sees all tags and we can
		// allocate big enough arrays
		// note: this constructor adds the
		// validation trees into the tagNumberer as a side effect, which is
		// important
		new StateSetTreeList(trainTrees, nSub, true, tagNumberer);
		new StateSetTreeList(validationTrees, nSub, true, tagNumberer);

		StateSetTreeList.initializeTagNumberer(trainTrees, tagNumberer);
		StateSetTreeList.initializeTagNumberer(validationTrees, tagNumberer);

		short[] nSubStateArray = new short[(short) tagNumberer.total()];
		Arrays.fill(nSubStateArray, nSubStates);
		// System.out.println("Everything is split in two except for the root.");
		nSubStateArray[0] = 1; // that's the ROOT
		return nSubStateArray;
	}

}
