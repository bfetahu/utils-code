package utils;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.time.SUTime;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.time.TimeExpression;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import gnu.trove.TIntArrayList;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.*;

/**
 * Created by besnik on 22/09/2014.
 */
public class NLPUtils {

    private StanfordCoreNLP pipeline;
    private Properties props;

    public NLPUtils(int load_case) {
        pipeline = loadNLPTools(load_case);
    }

    /**
     * Creates the StanfordCoreNLP for the Co-reference resolution and NER of text.
     *
     * @return
     */
    private StanfordCoreNLP loadNLPTools(int load_case) {
        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
        Properties props = new Properties();
        if (load_case == 0) {
            props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            return pipeline;
        } else if (load_case == 1) {
            props.put("annotators", "tokenize, ssplit, parse, pos, lemma");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

            return pipeline;
        } else if (load_case == 10) {
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            pipeline.addAnnotator(new TokenizerAnnotator(false));
            pipeline.addAnnotator(new WordsToSentencesAnnotator(false));
            pipeline.addAnnotator(new POSTaggerAnnotator(false));
            pipeline.addAnnotator(new TimeAnnotator("sutime", props));


            return pipeline;
        } else if (load_case == 2) {
            props.put("annotators", "tokenize,ssplit");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            return pipeline;
        } else if (load_case == 3) {
            props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            return pipeline;
        } else {
            props.put("annotators", "tokenize, ssplit, pos, lemma");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            return pipeline;
        }
    }

    /**
     * Extract the temporal expressions from a sentences.
     *
     * @param text
     * @param date
     * @return
     */
    public List<SUTime.Temporal> getTemporalExpressions(String text, String date) {
        Annotation annotation = new Annotation(text);
        annotation.set(CoreAnnotations.DocDateAnnotation.class, date);
        pipeline.annotate(annotation);
        List<SUTime.Temporal> temp_expr = new ArrayList<>();
        List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
        for (CoreMap cm : timexAnnsAll) {
            SUTime.Temporal temp = cm.get(TimeExpression.Annotation.class).getTemporal();
            temp_expr.add(temp);
        }
        return temp_expr;
    }

    /**
     * Annotates a document with POS and returns the corresponding JSON representation split into different paragraphs.
     *
     * @param text
     * @param doc_id
     * @return
     */
    public String getDocumentAnnotations(String text, String doc_id) {
        // create an empty Annotation just with the given text
        //split the document into paragraphs
        String[] paragraphs = text.split("\n+");
        //paragraph string buffers
        Map<Integer, StringBuffer> prg_sb = new TreeMap<>();

        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph_text = paragraphs[i];
            Annotation document = new Annotation(paragraph_text);
            pipeline.annotate(document);

            StringBuffer sb = prg_sb.get(i);
            sb = sb == null ? new StringBuffer() : sb;
            prg_sb.put(i, sb);

            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
            for (CoreMap sentence : sentences) {
                // traversing the words in the current sentence
                // a CoreLabel is a CoreMap with additional token-specific methods
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    // this is the text of the token
                    String word = token.get(CoreAnnotations.TextAnnotation.class);
                    // this is the POS tag of the token
                    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    sb.append(word).append("_").append(pos).append(" ");
                }
            }
        }


        //for each of the paragraphs construct the corresponding JSON output.
        StringBuffer sb = new StringBuffer();
        sb.append("\t{\n\t\t\"url\":\"").append(doc_id).append("\",");
        sb.append("\n\t\t\"annotated_paragraphs\": [");

        int counter = 0;
        for (int paragraph_id : prg_sb.keySet()) {
            if (counter != 0) {
                sb.append(", ");
            }
            String prg_str = prg_sb.get(paragraph_id).toString();
            sb.append("\n\t\t\t{\n\t\t\t\t\"id\":").append(paragraph_id).append(",");
            sb.append("\n\t\t\t\t\"annotated_content\":\"").append(StringEscapeUtils.escapeJson(prg_str)).append("\"");
            sb.append("\n\t\t\t}");

            counter++;
        }
        sb.append("\n\t\t]\n\t}");
        return sb.toString();
    }

    /**
     * Annotates a document with POS and returns the corresponding JSON representation split into different paragraphs.
     *
     * @param text
     * @return
     */
    public String getDocumentAnnotations(String text) {
        // create an empty Annotation just with the given text
        //split the document into paragraphs

        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        StringBuffer sb = new StringBuffer();
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                sb.append(word).append("_").append(pos).append(" ");
            }
            sb.append("\n");

        }
        return sb.toString().trim();
    }


    /**
     * Annotates a document with POS and returns the corresponding JSON representation split into different paragraphs.
     *
     * @param text
     * @param doc_id
     * @return
     */

    public String getDocumentAnnotationSingle(String text, String doc_id) {
        // create an empty Annotation just with the given text
        //paragraph string buffers
        StringBuffer sb = new StringBuffer();
        Annotation document = new Annotation(text);
        pipeline.annotate(document);


        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                sb.append(word).append("_").append(pos).append(" ");
            }
        }

        return sb.toString();
    }

    /**
     * Returns the set of Named Entities from a document. The NE are stored into the datastructure as <paragraph_id, <sentence_id, <named_entity, entity_type>>>
     *
     * @param text
     * @return
     */
    public Map<Integer, Map<Integer, List<Map.Entry<String, String>>>> loadDocumentAnnotations(String text) {
        // create an empty Annotation just with the given text
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        //store the statistics for each paragraph, i.e. the number of named entities
        Map<Integer, Map<Integer, List<Map.Entry<String, String>>>> prg = new HashMap<>();

        //analyse the individual paragraphs of the document
        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            getNamedEntities(sentence, prg);
        }

        //get first the coreference chain before analysing the number of named entities within a paragraph.
        Map<Integer, CorefChain> coref_chain = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
        //store the different tokens that are resolved into one of the named entities
        for (int coref_chain_idx : coref_chain.keySet()) {
            CorefChain coref = coref_chain.get(coref_chain_idx);
            if (coref.getRepresentativeMention().mentionType != Dictionaries.MentionType.PROPER && coref.getRepresentativeMention().mentionType != Dictionaries.MentionType.PRONOMINAL) {
                continue;
            }
            int snt_idx = coref.getRepresentativeMention().sentNum;
            Map.Entry<String, String> named_entity = null;
            for (int prg_idx : prg.keySet()) {
                if (named_entity == null) {
                    if (prg.get(prg_idx).containsKey(snt_idx)) {
                        for (Map.Entry<String, String> ne : prg.get(prg_idx).get(snt_idx)) {
                            if (ne.getKey().contains(coref.getRepresentativeMention().mentionSpan) || coref.getRepresentativeMention().mentionSpan.contains(ne.getKey())) {
                                named_entity = ne;
                                break;
                            }
                        }
                    }
                } else {
                    break;
                }
            }

            if (named_entity != null) {
                for (IntPair pair : coref.getMentionMap().keySet()) {
                    if (pair.getSource() == pair.getTarget()) {
                        continue;
                    }
                    for (CorefChain.CorefMention coref_mention : coref.getMentionMap().get(pair)) {
                        if (coref_mention.mentionType == Dictionaries.MentionType.PRONOMINAL) {
                            String co_ref_token = coref_mention.mentionSpan;
                            snt_idx = coref_mention.sentNum;

                            //get the paragraph of the co-refered token.
                            for (int prg_idx : prg.keySet()) {
                                if (prg.get(prg_idx).containsKey(snt_idx)) {
                                    prg.get(prg_idx).get(snt_idx).add(new AbstractMap.SimpleEntry<String, String>(named_entity.getKey() + "/" + co_ref_token, named_entity.getValue()));
                                }
                            }
                        }
                    }
                }
            }
        }
        return prg;
    }

    /**
     * Extracts the set of tokens which compose a named entity. It stores teh NE into paragraph and sentence data structures.
     *
     * @param sentence
     * @param prg
     */
    private void getNamedEntities(CoreMap sentence, Map<Integer, Map<Integer, List<Map.Entry<String, String>>>> prg) {
        //check the individual tokens for NE
        String named_entity = "";
        String named_entity_tag = "";

        for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
            //get the word and its NE in case it exists
            String token_value = token.get(CoreAnnotations.TextAnnotation.class);
            String token_NE = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

            if (!token_NE.equals("O")) {
                if (!named_entity.trim().isEmpty() && !named_entity_tag.equals(token_NE)) {
                    int prg_idx = token.get(CoreAnnotations.ParagraphAnnotation.class);
                    int snt_idx = token.get(CoreAnnotations.SentenceIndexAnnotation.class) + 1;

                    Map<Integer, List<Map.Entry<String, String>>> sub_prg_ne = prg.get(prg_idx);
                    sub_prg_ne = sub_prg_ne == null ? new HashMap<Integer, List<Map.Entry<String, String>>>() : sub_prg_ne;
                    prg.put(prg_idx, sub_prg_ne);

                    List<Map.Entry<String, String>> snt_sub_prg_ne = sub_prg_ne.get(snt_idx);
                    snt_sub_prg_ne = snt_sub_prg_ne == null ? new ArrayList<Map.Entry<String, String>>() : snt_sub_prg_ne;
                    sub_prg_ne.put(snt_idx, snt_sub_prg_ne);

                    snt_sub_prg_ne.add(new AbstractMap.SimpleEntry<String, String>(named_entity.trim(), named_entity_tag));
                    named_entity = "";
                }
                named_entity = named_entity + " " + token_value;
                named_entity_tag = token_NE;
            } else if (token_NE.equals("O")) {
                if (!named_entity.trim().isEmpty()) {
                    int prg_idx = token.get(CoreAnnotations.ParagraphAnnotation.class);
                    int snt_idx = token.get(CoreAnnotations.SentenceIndexAnnotation.class) + 1;

                    Map<Integer, List<Map.Entry<String, String>>> sub_prg_ne = prg.get(prg_idx);
                    sub_prg_ne = sub_prg_ne == null ? new HashMap<Integer, List<Map.Entry<String, String>>>() : sub_prg_ne;
                    prg.put(prg_idx, sub_prg_ne);

                    List<Map.Entry<String, String>> snt_sub_prg_ne = sub_prg_ne.get(snt_idx);
                    snt_sub_prg_ne = snt_sub_prg_ne == null ? new ArrayList<Map.Entry<String, String>>() : snt_sub_prg_ne;
                    sub_prg_ne.put(snt_idx, snt_sub_prg_ne);

                    snt_sub_prg_ne.add(new AbstractMap.SimpleEntry<String, String>(named_entity.trim(), named_entity_tag));
                    named_entity = "";
                }
            }
        }
    }

    /**
     * Annotates a document with POS and returns the corresponding JSON representation split into different paragraphs.
     *
     * @param text
     * @return
     */
    public List<String> getDocumentSentences(String text) {
        List<String> sentences_list = new ArrayList<>();
        // create an empty Annotation just with the given text
        //split the document into paragraphs
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        StringBuffer sb = new StringBuffer();
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            sentences_list.add(sentence.toString());
        }
        return sentences_list;
    }

    /**
     * Compute the sentiment score for a given piece of text. The sentiment score is computed sentence wise.
     *
     * @param text
     * @return
     */
    public TIntArrayList getSentimentScore(String text) {
        TIntArrayList sent_scores = new TIntArrayList();
        Annotation annotation = pipeline.process(text);
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
            sent_scores.add(sentiment);
        }
        return sent_scores;
    }
}
