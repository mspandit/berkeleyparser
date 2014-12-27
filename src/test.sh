#!/bin/sh
cd bin; jar cf ../BerkeleyParser_fat.jar edu/berkeley/nlp/*/*.class edu/berkeley/nlp/*/*/*.class; cd ..
java -cp BerkeleyParser_fat.jar edu.berkeley.nlp.PCFGLA.GrammarTrainer -path dev -out training_output -treebank SINGLEFILE
java -cp BerkeleyParser_fat.jar edu.berkeley.nlp.PCFGLA.WriteGrammarToTextFile training_output training_output.txt
echo "NO FURTHER OUTPUT EXPECTED"
diff training_output_baseline.txt.grammar training_output.txt.grammar
diff training_output_baseline.txt.lexicon training_output.txt.lexicon
diff training_output_baseline.txt.splits training_output.txt.splits
diff training_output_baseline.txt.words training_output.txt.words
rm training_output training_output.txt.* training_output_1* training_output_2* training_output_3* training_output_4* training_output_5* training_output_6*
