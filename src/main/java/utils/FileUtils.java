package utils;

import gnu.trove.TIntIntHashMap;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtils {
    /*
     * Gets the set of file names, from any directory structure recursively.
     */

    public static void getFilesList(String path, Set<String> filelist) {
        File dir = new File(path);
        if (dir.isFile()) {
            filelist.add(path);
        } else if (dir.isDirectory()) {
            String[] list = dir.list();
            for (String item : list) {
                getFilesList(path + "/" + item, filelist);
            }
        }
    }

    /*
     * Gets the set of file names, from any directory structure recursively.
     */
    public static void getFilesList(String path, Set<String> filelist, String filter) {
        File dir = new File(path);
        if (dir.isFile()) {
            filelist.add(path);
        } else if (dir.isDirectory()) {
            String[] list = dir.list();
            for (String item : list) {
                if (!item.contains(filter)) {
                    continue;
                }

                getFilesList(path + "/" + item, filelist);
            }
        }
    }

    /*
     * Write textual content into a file.
     */
    public static void addTextualContentToFile(String content, String path) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, true));
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Saves textual data content, into a file.
     */
    public static void saveText(String text, String path) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.append(text);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveText(String text, String path, boolean append, boolean compressed) {
        if (!compressed) {
            saveText(text, path, append);
        } else {
            saveCompressedText(text, path, append);
        }

    }

    public static void saveCompressedText(String text, String path, boolean append) {
        BufferedWriter writer = null;
        try {
            GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(new File(path), append));
            writer = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));

            writer.write(text);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Saves textual data content, into a file.
     */
    public static void saveText(String text, String path, boolean append) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, append));
            writer.append(text);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Writes the content of an object, into a file.
     */
    public static void saveObject(Object obj, String path) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
            out.writeObject(obj);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean fileExists(String path, boolean isDebug) {
        boolean rst = new File(path).exists();

        if (!rst && isDebug) {
            System.out.println("File doesnt exist... [" + path + "]");
        }

        return rst;
    }

    /*
     * Creates a new file at a specified location.
     */
    public static boolean createFile(String path) {
        try {
            File file = new File(path);
            return file.createNewFile();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return false;
    }

    /*
     * Reads the object content from a file, and later from the called method is 
     * casted to the correct type.
     */
    public static Object readObject(String path) {
        if (fileExists(path, false)) {
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
                Object obj = in.readObject();

                in.close();
                return obj;
            } catch (Exception e) {
                System.out.println("Error at file: " + path);
                System.out.println(e.getMessage());
            }
        }
        return null;
    }

    /*
     * Saves in textual file the content of enriched entities.
     */
    public static void saveAnnotationsAsText(HashMap<Integer, HashMap<String, String>> enrichedtokens, HashMap<Integer, HashMap<String, Set<String>>> tokens, String path) {
        StringBuffer sb = new StringBuffer();

        for (int k : enrichedtokens.keySet()) {
            HashMap<String, String> annotatedtokens = enrichedtokens.get(k);
            HashMap<String, Set<String>> subtokens = tokens.get(k);
            if (subtokens == null) {
                continue;
            }

            sb.append("\nTokens of size k: " + k + "\n");
            for (String token : annotatedtokens.keySet()) {
                Set<String> indexes = subtokens.get(token);
                if (indexes != null && indexes.size() >= 1) {
                    sb.append(token);
                    sb.append("\t");
                    sb.append(annotatedtokens.get(token));
                    sb.append("\n");
                }
            }
        }

        saveText(sb.toString(), path);
    }

    /*
     * Reads the textual contents from a file.
     * 
     */
    public static String readText(String path) {
        try {
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = getFileReader(path);
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.startsWith("#")) {
                    continue;
                }

                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            System.out.printf("Error reading file %s with message %s\n", path, e.getMessage());
        }
        return "";
    }

    /*
   * Reads the textual contents from a file.
   *
   */
    public static List<String> readLargeText(String path) {
        List<String> lst_results = new ArrayList<String>();
        try {
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
            int counter = 0;
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.startsWith("#")) {
                    continue;
                }

                sb.append(line);
                sb.append("\n");
                counter += 2;

                if (counter == 10000) {
                    lst_results.add(sb.toString());
                    sb.delete(0, sb.length());
                    counter = 0;
                }
            }

            lst_results.add(sb.toString());
            reader.close();
            return lst_results;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static BufferedReader getFileReader(String path) {
        try {
            if (path.endsWith(".gz")) {
                return getCompressedFileReader(path);
            } else if (path.endsWith("bz2")) {
                return getBZip2CompressedFileReader(path);
            } else if (path.toLowerCase().endsWith("zip")) {
                return getZIPCompressedFileReader(path);
            }
            return new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static BufferedReader getBZip2CompressedFileReader(String path) throws IOException, CompressorException {

        FileInputStream in = new FileInputStream(path);
        BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);

        return new BufferedReader(new InputStreamReader(bzIn));

    }

    public static BufferedReader getCompressedFileReader(String data_path) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(data_path))));
            return reader;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static BufferedReader getZIPCompressedFileReader(String data_path) {
        try {
            ZipFile zipFile = new ZipFile(data_path);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                InputStream stream = zipFile.getInputStream(entry);
                return new BufferedReader(new InputStreamReader(stream));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String readText(String path, String utf) {
        try {
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), utf));
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.startsWith("#")) {
                    continue;
                }

                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String readText(String path, boolean compressed) {
        try {
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = null;
            if (compressed) {
                reader = getCompressedFileReader(path);
            } else {
                reader = getFileReader(path);
            }

            while (reader.ready()) {
                String line = reader.readLine();
                if (line.startsWith("#")) {
                    continue;
                }

                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Reads the content of a file into a matrix.
     *
     * @param path
     * @param delim
     * @return
     */
    public static Map<String, Map<String, Double>> readMatrix(String path, String delim) {
        Map<String, Map<String, Double>> matrix = new TreeMap<String, Map<String, Double>>();
        String[] str_lines = FileUtils.readText(path).split("\n");
        for (String line : str_lines) {
            String[] data = line.split(delim);
            Map<String, Double> sub_matrix = matrix.get(data[0]);
            sub_matrix = sub_matrix == null ? new TreeMap<String, Double>() : sub_matrix;
            matrix.put(data[0], sub_matrix);

            sub_matrix.put(data[1], Double.parseDouble(data[2]));
        }
        return matrix;
    }

    /*
     * Reads the textual contents from a file into  a set split based on a specific delimeter.
     */

    public static Set<String> readIntoSet(String path, String delim, boolean changeCase) {
        if (!FileUtils.fileExists(path, true)) {
            return null;
        }

        Set<String> rst = new HashSet<String>();

        String content = readText(path);
        String[] tmp = content.split(delim);
        for (String s : tmp) {
            if (changeCase) {
                rst.add(s.trim().toLowerCase());
            } else {
                rst.add(s);
            }
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a set split based on a specific delimeter.
     */

    public static Set<Integer> readIntoIntSet(String path, String delim) {
        if (!FileUtils.fileExists(path, true)) {
            return null;
        }

        Set<Integer> rst = new HashSet<Integer>();

        String content = readText(path);
        String[] tmp = content.split(delim);
        for (String s : tmp) {
            try {
                rst.add(Integer.valueOf(s.trim()));
            } catch (Exception e) {
                System.out.printf("[UTILS] Error converting to int value %s\n", s);
            }
        }

        return rst;
    }

     /*
     * Reads the textual contents from a file into  a set split based on a specific delimeter.
     */

    public static Set<String> readIntoSet(String path, String delim, boolean changeCase, boolean compressed) {
        if (!FileUtils.fileExists(path, true)) {
            return null;
        }

        Set<String> rst = new HashSet<String>();

        String content = readText(path, compressed);
        String[] tmp = content.split(delim);
        for (String s : tmp) {
            if (changeCase) {
                rst.add(s.trim().toLowerCase());
            } else {
                rst.add(s);
            }
        }

        return rst;
    }

     /*
     * Reads the textual contents from a file into  a set split based on a specific delimeter.
     */

    public static Map<String, Integer> readIntoIndexedSet(String path, String delim, boolean compressed) {
        if (!FileUtils.fileExists(path, true)) {
            return null;
        }

        Map<String, Integer> rst = new HashMap<String, Integer>();

        String content = readText(path, compressed);
        String[] tmp = content.split(delim);
        int counter = 0;
        for (String s : tmp) {
            rst.put(s, counter++);
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, String> readIntoStringMap(String path, String delim, boolean changeCase, boolean ignoreSchema) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<String, String> rst = new TreeMap<String, String>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            if (ignoreSchema && tmp.length != 2) {
                rst.put(tmp[0].trim(), null);
            } else if (tmp.length == 2) {
                rst.put(tmp[0].trim(), tmp[1]);
            }
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, String> readIntoStringMap(String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<String, String> rst = new TreeMap<String, String>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            if (tmp.length == 2) {
                rst.put(tmp[0].trim(), tmp[1]);
            }
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<Integer, String> readIntoIntStringMap(String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<Integer, String> rst = new HashMap<Integer, String>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            if (tmp.length == 2) {
                rst.put(Integer.valueOf(tmp[0].trim()), tmp[1]);
            }
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, Double> readIntoStringDoubleMap(String path, String delim, boolean changeCase) {
        String[] lines = readText(path).split("\n");
        Map<String, Double> rst = new HashMap<String, Double>();

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] tmp = line.split(delim);
            rst.put(tmp[0].trim(), Double.parseDouble(tmp[1]));
        }

        return rst;
    }


    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<Double, Double> readIntoDoubleMap(String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<Double, Double> rst = new TreeMap<Double, Double>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            rst.put(Double.parseDouble(tmp[0].trim()), Double.parseDouble(tmp[1]));
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, Integer> readIntoStringIntMap(String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<String, Integer> rst = new TreeMap<String, Integer>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            rst.put(tmp[0].trim(), Integer.valueOf(tmp[1]));
        }

        return rst;
    }
    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */

    public static TIntIntHashMap readIntoIntMap(String path, String delim) {
        String[] lines = readText(path).split("\n");
        TIntIntHashMap rst = new TIntIntHashMap();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            if (line.trim().isEmpty() || tmp.length != 2) {
                continue;
            }
            int key = Integer.valueOf(tmp[0]);
            int value = Integer.valueOf(tmp[1]);
            rst.put(key, value);
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */

    public static TIntIntHashMap readCompressedIntoIntMap(String path, String delim) {
        try {
            TIntIntHashMap rst = new TIntIntHashMap();
            BufferedReader reader = getCompressedFileReader(path);
            while (reader.ready()) {
                String line = reader.readLine();
                String[] tmp = line.split(delim);
                if (line.trim().isEmpty() || tmp.length != 2) {
                    continue;
                }
                int key = Integer.valueOf(tmp[0]);
                int value = Integer.valueOf(tmp[1]);
                rst.put(key, value);
            }

            return rst;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, String> readIntoStringMap(String path, String delim, boolean changeCase, String filter) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<String, String> rst = new TreeMap<String, String>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            if (!filter.contains(tmp[1])) {
                continue;
            }

            rst.put(tmp[0].trim(), tmp[1]);
        }

        return rst;
    }

    public static void getDirList(String path, Set<String> dirlist) {
        File dir = new File(path);
        File[] dirs = dir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
        for (File dir_tmp : dirs) {
            dirlist.add(dir_tmp.getName());
        }
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, Entry<String, String>> readIntoEntryMap(String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<String, Entry<String, String>> rst = new TreeMap<String, Entry<String, String>>();

        for (String line : lines) {

            String[] tmp = line.split(delim);
            if (tmp.length < 3) {
                continue;
            }

            Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(tmp[1], tmp[2]);
            rst.put(tmp[0], entry);
        }
        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static void readIntoEntryMap1(Map<String, Entry<String, String>> rst, String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);

        for (String line : lines) {
            String[] tmp = line.split(delim);
            if (tmp.length < 3) {
                continue;
            }

            Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(tmp[1], tmp[2]);
            rst.put(tmp[0], entry);

            System.out.println(rst.size() + "\t" + line);
        }
        System.out.println(rst.size());
    }


    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, Entry<String, String>> readIntoEntryMap(Map<String, Entry<String, String>> rst, String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);

        for (String line : lines) {

            String[] tmp = line.split(delim);
            if (tmp.length < 3) {
                continue;
            }

            Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(tmp[1], tmp[2]);
            rst.put(tmp[0], entry);
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a set split based on a specific delimeter.
     */
    public static Set<String> readIntoSetFileNames(String path) {
        Set<String> rst = new HashSet<String>();

        Set<String> tmp = new HashSet<String>();
        getFilesList(path, tmp);

        for (String file : tmp) {
            String s = file.substring(file.lastIndexOf("/") + 1).trim();
            rst.add(s);
        }

        return rst;
    }

    /**
     * Reads a text file into a map data structures. The structure is the
     * following item_i = x1,x2...
     *
     * @param path
     * @param delim
     * @param delim_1
     * @return
     */
    public static Map<String, List<String>> readMapList(String path, String delim, String delim_1) {
        String[] lines = readText(path).split("\n");
        Map<String, List<String>> map_list = new TreeMap<String, List<String>>();

        for (String line : lines) {
            String[] data = line.split(delim);

            List<String> list = new ArrayList<String>();
            map_list.put(data[0], list);

            String[] data_items = data[1].split(delim_1);
            for (String data_item : data_items) {
                list.add(data_item);
            }
        }
        return map_list;
    }

    /**
     * Reads a text file into a map data structures. The structure is the
     * following item_i = x1,x2...
     *
     * @param path
     * @param delim
     * @param delim_1
     * @return
     */
    public static Map<String, Set<String>> readMapSet(String path, String delim, String delim_1) {
        String[] lines = readText(path).split("\n");
        Map<String, Set<String>> map_list = new TreeMap<>();

        for (String line : lines) {
            String[] data = line.split(delim);

            Set<String> list = new HashSet<>();
            map_list.put(data[0], list);

            String[] data_items = data[1].split(delim_1);
            for (String data_item : data_items) {
                list.add(data_item);
            }
        }
        return map_list;
    }

    /**
     * Reads a text file into a map data structures. The structure is the
     * following item_i = x1,x2...
     *
     * @param path
     * @param delim
     * @return
     */
    public static Map<String, Set<String>> readMapSet(String path, String delim) {
        String[] lines = readText(path).split("\n");
        Map<String, Set<String>> map_list = new HashMap<>();

        for (String line : lines) {
            String[] data = line.split(delim);
            if (data.length != 2) {
                continue;
            }
            Set<String> list = map_list.get(data[0]);
            list = list == null ? new HashSet<String>() : list;
            map_list.put(data[0], list);

            list.add(data[1]);
        }
        return map_list;
    }

    /**
     * Reads a text file into a map data structures. The structure is the
     * following item_i = x1,x2...
     *
     * @param path
     * @param delim
     * @return
     */
    public static Map<Integer, Set<Integer>> readIntMapSet(String path, String delim) {
        String[] lines = readText(path).split("\n");
        Map<Integer, Set<Integer>> map_list = new HashMap<Integer, Set<Integer>>();

        for (String line : lines) {
            String[] data = line.split(delim);
            if (data.length != 2) {
                continue;
            }

            int key = Integer.valueOf(data[0]);
            int val = Integer.valueOf(data[1]);

            Set<Integer> list = map_list.get(key);
            list = list == null ? new HashSet<Integer>() : list;
            map_list.put(key, list);

            list.add(val);
        }
        return map_list;
    }

    /**
     * Reads a text file into a map data structures. The structure is the
     * following item_i = x1,x2...
     *
     * @param path
     * @param delim
     * @return
     */
    public static Map<Integer, Set<String>> readIntoIntMapSet(String path, String delim) {
        if (!FileUtils.fileExists(path, false)) {
            return new HashMap<Integer, Set<String>>();
        }
        String[] lines = readText(path).split("\n");
        Map<Integer, Set<String>> map_list = new HashMap<Integer, Set<String>>();

        for (String line : lines) {
            String[] data = line.split(delim);
            if (data.length != 2) {
                continue;
            }

            int key = Integer.valueOf(data[0]);
            String val = data[1];

            Set<String> list = map_list.get(key);
            list = list == null ? new HashSet<String>() : list;
            map_list.put(key, list);

            list.add(val);
        }
        return map_list;
    }


    /*
     * Checks whether a directory exists or not. If not, then it creates the directory.
     */

    public static void checkDir(String path) {
        File dir = new File(path);

        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    public static boolean fileDirExists(String path) {
        File dir = new File(path);
        return dir.exists();
    }

    public static org.w3c.dom.Document readXMLDocument(String path) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            String file_content = FileUtils.readText(path);

            InputSource is = new InputSource(new StringReader(file_content));
            org.w3c.dom.Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            return doc;
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static org.w3c.dom.Document readXMLDocumentFromString(String content) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            InputSource is = new InputSource(new StringReader(content));
            org.w3c.dom.Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            return doc;
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static Map<Integer, String> readIntoStringIntMapReverse(String section_doc_indices_path, String s, boolean b) {
        Map<String, Integer> rst = readIntoStringIntMap(section_doc_indices_path, s, b);
        Map<Integer, String> rst_final = new HashMap<Integer, String>();

        for (String key : rst.keySet()) {
            rst_final.put(rst.get(key), key);
        }
        return rst_final;
    }
}
