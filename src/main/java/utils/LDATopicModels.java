package utils;

import cc.mallet.pipe.*;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import org.tartarus.snowball.ext.EnglishStemmer;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;


/**
 * Created by besnik on 10/1/14.
 */
public class LDATopicModels {
    //this is used in all cases, hence we provide it in the constructor.
    private TokenSequenceRemoveStopwords stop_words_seq;

    public LDATopicModels(String stop_words_path) {
        LogManager.getLogManager().reset();

        Logger globalLogger = Logger.getLogger("global");
        Handler[] handlers = globalLogger.getHandlers();
        for (Handler handler : handlers) {
            globalLogger.removeHandler(handler);
        }


        //load the stop words only once
        stop_words_seq = new TokenSequenceRemoveStopwords();
        String[] stop_words_terms = FileUtils.readText(stop_words_path).split("\n");
        stop_words_seq.addStopWords(stop_words_terms);
    }


    /**
     * Given a document compute the topic models and output the corresponding results
     * showing the topic ids, top words and their corresponding probability distributions.
     * <p/>
     * The data for which we need to perform the topic modelling needs to be in the form of triples:
     * document_id document_class document_content
     *
     * @param topic_words the number of words which we use for analysis per topic.
     * @throws IOException
     */
    public List<Map.Entry<String, Double>> computeTopicModel(String doc_text, int topic_words, int iterations) {
        List<Map.Entry<String, Double>> topic_keywords = new ArrayList<>();
        try {
            // Begin by importing documents from text to feature sequences
            ArrayList<Pipe> pipeList = new ArrayList<>();

            // Pipes: lowercase, tokenize, remove stopwords, map to features
            pipeList.add(new CharSequenceLowercase());
            pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
            pipeList.add(stop_words_seq);
            pipeList.add(new TokenSequence2FeatureSequence());

            InstanceList instances = new InstanceList(new SerialPipes(pipeList));
            //if the document doesnt belong to the given domain and section, discard it
            instances.addThruPipe(new Instance(doc_text, "X", "X", null));

            // Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
            //  Note that the first parameter is passed as the sum over topics, while
            //  the second is the parameter for a single dimension of the Dirichlet prior.
            int numTopics = 1;
            ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);

            model.addInstances(instances);
            model.setNumIterations(iterations);
            model.estimate();

            // The data alphabet maps word IDs to strings
            Alphabet dataAlphabet = instances.getDataAlphabet();

            // Get an array of sorted sets of word ID/count pairs
            ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
            for (int topic = 0; topic < numTopics; topic++) {
                Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();

                int rank = 0;
                while (iterator.hasNext() && rank < topic_words) {
                    IDSorter idCountPair = iterator.next();
                    topic_keywords.add(new AbstractMap.SimpleEntry<>(dataAlphabet.lookupObject(idCountPair.getID()).toString(), idCountPair.getWeight()));
                    rank++;
                }
            }
        } catch (Exception e) {
            System.out.printf("Exception while computing topics with error %s.\n", e.getMessage());
        }
        return topic_keywords;
    }

    /**
     * Given a document compute the topic models and output the corresponding results
     * showing the topic ids, top words and their corresponding probability distributions.
     * <p/>
     * The data for which we need to perform the topic modelling needs to be in the form of triples:
     * document_id document_class document_content
     *
     * @throws IOException
     */
    public Set<String> computeTopicModel(ParallelTopicModel model) {
        Set<String> topic_keywords = new HashSet<>();
        try {
            // The data alphabet maps word IDs to strings
            Alphabet dataAlphabet = model.getAlphabet();

            // Get an array of sorted sets of word ID/count pairs
            ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
            for (int topic = 0; topic < model.numTopics; topic++) {
                Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();

                int rank = 0;
                while (iterator.hasNext() && rank < model.wordsPerTopic) {
                    IDSorter idCountPair = iterator.next();
                    topic_keywords.add(dataAlphabet.lookupObject(idCountPair.getID()).toString());
                    rank++;
                }
            }
        } catch (Exception e) {
            System.out.printf("Exception while computing topics with error %s.\n", e.getMessage());
        }
        return topic_keywords;
    }

    /**
     * Given a document compute the topic models and output the corresponding results
     * showing the topic ids, top words and their corresponding probability distributions.
     * <p/>
     * The data for which we need to perform the topic modelling needs to be in the form of triples:
     * document_id document_class document_content
     *
     * @throws IOException
     */
    public Map<Integer, Set<String>> computeTopicModel(ParallelTopicModel model, int words) {
        Map<Integer, Set<String>> topic_keywords = new HashMap<>();
        try {
            // The data alphabet maps word IDs to strings
            Alphabet dataAlphabet = model.getAlphabet();

            // Get an array of sorted sets of word ID/count pairs
            ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
            for (int topic = 0; topic < model.numTopics; topic++) {
                Set<String> terms = new LinkedHashSet<>();
                topic_keywords.put(topic, terms);
                Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
                EnglishStemmer stem = new EnglishStemmer();

                while (iterator.hasNext() && terms.size() < words) {
                    int term_id = iterator.next().getID();
                    String term = dataAlphabet.lookupObject(term_id).toString();
                    stem.setCurrent(term);
                    stem.stem();

                    terms.add(stem.getCurrent());
                }
            }
        } catch (Exception e) {
            System.out.printf("Exception while computing topics with error %s.\n", e.getMessage());
        }
        return topic_keywords;
    }


    public ParallelTopicModel computeDocumentTopicModel(String doc_text, int iterations) {
        try {
            // Begin by importing documents from text to feature sequences
            ArrayList<Pipe> pipeList = new ArrayList<>();

            // Pipes: lowercase, tokenize, remove stopwords, map to features
            pipeList.add(new CharSequenceLowercase());
            pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
            pipeList.add(stop_words_seq);
            pipeList.add(new TokenSequence2FeatureSequence());

            InstanceList instances = new InstanceList(new SerialPipes(pipeList));
            //if the document doesnt belong to the given domain and section, discard it
            instances.addThruPipe(new Instance(doc_text, "X", "X", doc_text));

            // Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
            //  Note that the first parameter is passed as the sum over topics, while
            //  the second is the parameter for a single dimension of the Dirichlet prior.
            int numTopics = 1;
            ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);


            model.addInstances(instances);
            model.setNumIterations(iterations);
            model.estimate();
            return model;
        } catch (Exception e) {
            System.out.printf("Exception while computing topics with error %s.\n", e.getMessage());
        }
        return null;
    }

    public ParallelTopicModel computeDocumentTopicModel(Map<Integer, String> documents, int iterations, int topics) {
        try {
            // Begin by importing documents from text to feature sequences
            ArrayList<Pipe> pipeList = new ArrayList<>();

            // Pipes: lowercase, tokenize, remove stopwords, map to features
            pipeList.add(new CharSequenceLowercase());
            pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
            pipeList.add(stop_words_seq);
            pipeList.add(new TokenSequence2FeatureSequence());

            InstanceList instances = new InstanceList(new SerialPipes(pipeList));
            //if the document doesnt belong to the given domain and section, discard it
            for (int doc_id : documents.keySet()) {
                try {
                    instances.addThruPipe(new Instance(documents.get(doc_id), doc_id, "X", null));
                } catch (Exception e) {
                    System.out.printf("Error adding instance through pipe for LDA: %d with content %s and error message %s.\n", doc_id, documents.get(doc_id), e.getMessage());
                }
            }

            // Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
            //  Note that the first parameter is passed as the sum over topics, while
            //  the second is the parameter for a single dimension of the Dirichlet prior.
            int numTopics = topics;
            ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);

            model.addInstances(instances);
            model.setNumIterations(iterations);
            model.estimate();
            return model;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Load the trained topic model for a specific domain and section.
     *
     * @return
     * @throws Exception
     */

    public ParallelTopicModel loadTopicModel(String topic_file) throws Exception {
        ParallelTopicModel model = ParallelTopicModel.read(new File(topic_file));
        return model;
    }

    /**
     * Estimate the probability of a test document to belong to one of the trained sections for a given domain.
     *
     * @param model
     * @return
     */
    public double assessTestDocument(String doc_text, ParallelTopicModel model) {
        // Begin by importing documents from text to feature sequences
        ArrayList<Pipe> pipeList = new ArrayList<>();
        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(stop_words_seq);
        pipeList.add(new TokenSequence2FeatureSequence());

        InstanceList instances = new InstanceList(new SerialPipes(pipeList));


        Instance test_doc_instance = new Instance(doc_text, null, "X", null);
        instances.addThruPipe(test_doc_instance);
        TopicInferencer inferencer = model.getInferencer();
        double[] probs = inferencer.getSampledDistribution(instances.get(0), 100, 1, 5);

        return probs[0];
    }
}

