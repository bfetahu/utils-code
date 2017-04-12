package utils;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.DependencyGrammar;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by besnik on 8/19/15.
 */
public class DocumentParsers {
    private LexicalizedParser lp;

    public DocumentParsers() {
        String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
        lp = LexicalizedParser.loadModel(parserModel);
    }

    public String parseSentence(String document) {
        // This option shows loading, sentence-segmenting and tokenizing

        // You could also create a tokenizer here (as below) and pass it
        // to DocumentPreprocessor

        Tree parse = lp.parse(document);
        return parse.toString();
    }

    public String parseSentenceI(String document) {
        // This option shows loading, sentence-segmenting and tokenizing

        // You could also create a tokenizer here (as below) and pass it
        // to DocumentPreprocessor
        try {
            for (List<HasWord> sentence : new DocumentPreprocessor(new StringReader(document))) {
                Tree parse = lp.apply(sentence);
                return parse.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String parseTaggedSentence(String sentence) {
        if (sentence == null || sentence.isEmpty()) {
            return null;
        }
        String[] tmp = sentence.split("\\s+");
        List<TaggedWord> sentence3 = new ArrayList<>();
        for (String s : tmp) {
            String[] key_tag = s.split("_");
            sentence3.add(new TaggedWord(key_tag[0].intern(), key_tag[1].intern()));
        }
        Tree parse = lp.parse(sentence3);
        return parse.toString();
    }


    public String parseDocument(String document) {
        // This option shows loading, sentence-segmenting and tokenizing

        // You could also create a tokenizer here (as below) and pass it
        // to DocumentPreprocessor
        StringBuffer sb = new StringBuffer();
        for (List<HasWord> sentence : new DocumentPreprocessor(new StringReader(document))) {
            Tree parse = lp.apply(sentence);
            sb.append(parse.toString()).append("\n");
        }
        return sb.toString();
    }
}
