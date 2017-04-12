package utils;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.linalg.Algebra;
import org.tartarus.snowball.ext.PorterStemmer;

import java.util.*;

/**
 * Created by besnik on 2/2/17.
 */
public class SimilarityMeasures {
    public static Algebra alg = new Algebra();
    public static Set<String> stop_words;
    public static boolean stem_words = true;

    /**
     * Compute the cosine similarity between two documents.
     *
     * @param doc_a
     * @param doc_b
     * @return
     */
    public static double computeCosineSimilarity(String doc_a, String doc_b) {
        Map<String, Integer> doc_a_counts = new HashMap<>();
        Map<String, Integer> doc_b_counts = new HashMap<>();

        updateWordFrequency(doc_a, doc_a_counts);
        updateWordFrequency(doc_b, doc_b_counts);

        return computeCosineSimilarity(doc_a_counts, doc_b_counts);
    }

    /**
     * Computes the cosine similarity between the word distributions between the tweets grouped based on specific hashtags.
     *
     * @param tag_a_word_freq
     * @param tag_b_word_freq
     * @return
     */
    public static double computeCosineSimilarity(Map<String, Integer> tag_a_word_freq, Map<String, Integer> tag_b_word_freq) {
        Set<String> keys = new HashSet<>(tag_a_word_freq.keySet());
        keys.addAll(tag_b_word_freq.keySet());

        //compute the normalized frequency for each word as #word_freq / #total_freq
        double[] tag_a_scores = new double[keys.size()];
        double[] tag_b_scores = new double[keys.size()];

        int index = 0;
        double total_freq_a = tag_a_word_freq.values().stream().mapToDouble(x -> x).sum();
        double total_freq_b = tag_b_word_freq.values().stream().mapToDouble(x -> x).sum();
        double sum_a = 0.0, sum_b = 0.0;

        for (String key : keys) {
            tag_a_scores[index] = tag_a_word_freq.containsKey(key) ? tag_a_word_freq.get(key) / total_freq_a : 0.0;
            tag_b_scores[index] = tag_b_word_freq.containsKey(key) ? tag_b_word_freq.get(key) / total_freq_b : 0.0;

            sum_a += Math.pow(tag_a_scores[index], 2);
            sum_b += Math.pow(tag_b_scores[index], 2);
            index++;
        }

        double dot_product = alg.mult(new DenseDoubleMatrix1D(tag_a_scores), new DenseDoubleMatrix1D(tag_b_scores));

        //compute the cosine similarity
        return dot_product / (Math.sqrt(sum_a) * Math.sqrt(sum_b));
    }

    /**
     * Compute the KL divergence between two distributions
     *
     * @param a This represents the Wikipedia entity word distribution
     * @param b this represents the distribution of the words in a specific time slice for a hashtag.
     * @return
     */
    public static double computeKLDivergence(Map<String, Double> a, Map<String, Double> b) {
        double rst = 0;
        double smoothing_factor = 1.0 / b.size();

        //iterate over the dictionary of the entity word distribution
        for (String key : a.keySet()) {
            double score_a = a.get(key);

            if (b.containsKey(key)) {
                double score_b = b.get(key);
                rst += score_a * Math.log(score_a / score_b);
            } else {
                rst += score_a * Math.log(score_a / smoothing_factor);
            }
        }
        return 1 - Math.exp(-rst);
    }


    /**
     * Compute the KL divergence between two distributions
     *
     * @param a This represents the Wikipedia entity word distribution
     * @param b this represents the distribution of the words in a specific time slice for a hashtag.
     * @return
     */
    public static double computeKLDivergenceInt(Map<String, Integer> a, Map<String, Integer> b) {
        Map<String, Double> a_score = new HashMap<>();
        Map<String, Double> b_score = new HashMap<>();

        double max_score_a = a.values().stream().mapToDouble(x -> x).sum();
        double max_score_b = b.values().stream().mapToDouble(x -> x).sum();

        a.keySet().forEach(key -> a_score.put(key, a.get(key) / max_score_a));
        b.keySet().forEach(key -> b_score.put(key, b.get(key) / max_score_b));

        return computeKLDivergence(a_score, b_score);
    }


    /**
     * Computes the KL divergence between the two unigram language models computed from the content of URLs shared from tweets
     * with a specific hashtag and the language model of the entity section.
     *
     * @param content_a
     * @param content_b
     * @return
     */
    public static double computeKLDivergence(String content_a, String content_b) {
        Map<String, Integer> term_freq_a = new HashMap<>();
        Map<String, Integer> term_freq_b = new HashMap<>();
        updateWordFrequency(content_a, term_freq_a);
        updateWordFrequency(content_b, term_freq_b);

        return computeKLDivergenceInt(term_freq_a, term_freq_b);
    }

    /**
     * Computes the jaccard distance between two sets of values.
     *
     * @param values_a
     * @param values_b
     * @return
     */
    public static double computeJaccardDistance(Set<?> values_a, Set<?> values_b) {
        if (values_a.isEmpty() || values_b.isEmpty()) {
            return 0;
        }
        Set<?> common = new HashSet<>(values_a);
        common.retainAll(values_b);
        if (stop_words != null && !stop_words.isEmpty()) {
            common.removeAll(stop_words);
        }

        return common.size() / (double) (values_a.size() + values_b.size() - common.size());
    }

    /**
     * Computes the jaccard distance between two strings
     *
     * @param text_a
     * @param text_b
     * @return
     */
    public static double computeJaccardDistance(String text_a, String text_b) {
        String[] tmp_a = text_a.toLowerCase().split("\\s+");
        String[] tmp_b = text_b.toLowerCase().split("\\s+");

        Set<String> values_a = new HashSet<>();
        Set<String> values_b = new HashSet<>();

        for (String key : tmp_a) {
            if (stop_words != null && stop_words.contains(key)) {
                continue;
            }
            values_a.add(key);
        }
        for (String key : tmp_b) {
            if (stop_words != null && stop_words.contains(key)) {
                continue;
            }
            values_b.add(key);
        }
        Set<String> common = new HashSet<>(values_a);
        common.retainAll(values_b);
        return common.size() / (double) (values_a.size() + values_b.size() - common.size());
    }

    /**
     * Updates the word frequency for a given tag.
     *
     * @param text
     * @param freq
     */
    public static void updateWordFrequency(String text, Map<String, Integer> freq) {
        String[] tokens = text.toLowerCase().replaceAll("[^A-Za-z0-9]", " ").split("\\s+");
        PorterStemmer p = new PorterStemmer();
        for (String token : tokens) {
            if (token.trim().isEmpty() || (stop_words != null && stop_words.contains(token))) {
                continue;
            }
            String token_stemmed = token;
            if (stem_words) {
                p.setCurrent(token);
                p.stem();
                token_stemmed = p.getCurrent();
            }

            Integer count = freq.get(token_stemmed.intern());
            count = count == null ? 0 : count;
            count += 1;
            freq.put(token_stemmed, count);
        }
    }


    /**
     * Sample from a set.
     *
     * @param sample_size
     * @param data
     * @return
     */
    public static Set sampleInSet(int sample_size, Set<?> data) {
        if (data.size() <= sample_size) {
            return data;
        }
        Set samples = new HashSet<>();
        Object[] tweets = data.toArray();
        Random rand = new Random();
        while (samples.size() < sample_size) {
            int next = rand.nextInt(tweets.length);
            samples.add(tweets[next]);
        }
        return samples;
    }


    /**
     * Sample from a set.
     *
     * @param sample_size
     * @param data
     * @return
     */
    public static Set sampleInSet(int sample_size, List<?> data) {
        if (data.size() <= sample_size) {
            return new HashSet(data);
        }
        Set samples = new HashSet<>();
        Random rand = new Random();
        while (samples.size() < sample_size) {
            int next = rand.nextInt(data.size());
            samples.add(data.get(next));
        }
        return samples;
    }


    /**
     * Computes the jaccard distance from two sets.
     *
     * @param a
     * @param b
     * @return
     */
    public static double getWeightedJaccardDistance(Map<?, Integer> a, Map<?, Integer> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 1.0;
        }
        Set<Object> common = new HashSet<>();
        a.keySet().forEach(key -> common.add(key));
        b.keySet().forEach(key -> common.add(key));

        double jaccard_a = 0.0, jaccard_b = 0.0, jaccard_a_b = 0.0;

        for (Object key : common) {
            int a_val = a.containsKey(key) ? a.get(key) : 0;
            int b_val = b.containsKey(key) ? b.get(key) : 0;

            jaccard_a += a_val;
            jaccard_b += b_val;

            jaccard_a_b += Math.abs(a_val - b_val);
        }

        return 1 - (jaccard_a + jaccard_b - jaccard_a_b) / (jaccard_a + jaccard_b + jaccard_a_b);
    }

}
