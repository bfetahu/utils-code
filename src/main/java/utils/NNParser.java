package utils;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.GrammaticalStructure;

import java.io.StringReader;
import java.util.List;

/**
 * Created by besnik on 8/24/15.
 */
public class NNParser {
    public DependencyParser parser;

    public NNParser() {
        parser = DependencyParser.loadFromModelFile("edu/stanford/nlp/models/parser/nndep/PTB_Stanford_params.txt.gz");
    }

    public String parseSentence(String sentence_str) {
        DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(sentence_str));
        for (List<HasWord> sentence : tokenizer) {
            GrammaticalStructure gs = parser.predict(sentence);

            // Print typed dependencies
            return gs.root().toString();
        }

        return "";
    }
}
