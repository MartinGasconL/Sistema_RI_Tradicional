import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Paths;

/**
 * @author Martín Gascón 764429
 * @author Eduardo Ruiz  764539
 * Simple command-line based search demo.
 * */
public class SearchFiles {

  private String infoNeedsPath;
  private String outputPath;
  private SpanishNamesDetector nameDetector;
  private SpanishLocationsDetector locationDetector;

  private final float DESCRIPTION_INFONEED_WEIGHT = 5;
  private final float TITLE_INFONEED_WEIGHT =  4;
  private final float SUBJECT_INFONEED_WEIGHT = 3;
  private final float TYPE_WEIGHT = 10;
  private final float LOCATION_WEIGHT = 5;
  private final float NAME_CREATOR_WEIGHT = 10;
  private final float NAME_CONTRIBUTOR_WEIGHT = 10;
  private final float DESCRIPTION_NAME_WEIGHT = 10;
  private final float BEGIN_DATE_WEIGHT = 10;
  private final float END_DATE_WEIGHT = 10;


  /**
   * This class implements a traditional information recover system searcher
   * @param args  command line params
   * @throws IOException
   * @throws ParseException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  public SearchFiles(String args[]) throws IOException, ParseException, ParserConfigurationException, SAXException {

    nameDetector = new SpanishNamesDetector();
    locationDetector = new SpanishLocationsDetector();

    String usage =
            "Usage:\t-index INDEX_DIR_PATH -infoNeeds INFO_NEEDS_FILE_PATH -output OUTPUT_FILE_PATH\n\n";

      if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }

    String indexPath = "index";
    infoNeedsPath = "index";
    outputPath = "index";


    for(int i = 0;i < args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-infoNeeds".equals(args[i])) {
        infoNeedsPath = args[i+1];
        i++;
      } else if ("-output".equals(args[i])) {
        outputPath = args[i+1];
        i++;
      }
    }

    File f= new File(outputPath);
    f.delete();

    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new CustomAnalyzer();

    FileInputStream in = new FileInputStream(infoNeedsPath);
    org.w3c.dom.Document dc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
    NodeList nl = dc.getElementsByTagName("informationNeed");
    for (int i = 0; i < nl.getLength(); i++) {
      Node text = ((Element)nl.item(i)).getElementsByTagName("text").item(0);
      String infoNeedId = ((Element)nl.item(i)).getElementsByTagName("identifier").item(0).getTextContent();

      BooleanQuery query = prepareQuery(text.getTextContent(), analyzer);
      showResults(searcher,query);
      writeResults(searcher, query, infoNeedId);

    }
    reader.close();

  }

  /**
   * Prepare a boolean query with different weights for the different fields.
   * @param infoNeedPath  Path of the information needs file.
   * @param analyzer  Analizer to analize the query text content.
   * @return  A boolean query with queries of the different fields added.
   * @throws ParseException
   */
  private BooleanQuery prepareQuery(String infoNeedPath, Analyzer analyzer) throws ParseException {
    BoostQuery qDescription = new BoostQuery(new QueryParser("description", analyzer).parse(infoNeedPath),DESCRIPTION_INFONEED_WEIGHT);
    BoostQuery qTitle = new BoostQuery(new QueryParser("title", analyzer).parse(infoNeedPath), TITLE_INFONEED_WEIGHT);
    BoostQuery qSubject = new BoostQuery(new QueryParser("subject", analyzer).parse(infoNeedPath), SUBJECT_INFONEED_WEIGHT);

    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.add(qSubject,BooleanClause.Occur.SHOULD);
    builder.add(qDescription,BooleanClause.Occur.SHOULD);
    builder.add(qTitle,BooleanClause.Occur.SHOULD);

    queryNames(infoNeedPath, analyzer, builder);
    queryType(infoNeedPath, builder);
    queryLocations(infoNeedPath,  analyzer, builder);
    queryDates(infoNeedPath, builder);


    return builder.build();
  }

  /**
   * Add queries from the date field if detects any date pattern in the information need.
   * @param sentence  Sentence to query.
   * @param builder Builder of the main boolean query.
   */
  private void queryDates(String sentence, BooleanQuery.Builder builder) {
    DateDetector dateDetector = new DateDetector();
    DateDetector.Range range;
    if((range = dateDetector.getRangePattern(sentence)) != null){
      BoostQuery qBeginDate = new BoostQuery(DoublePoint.newRangeQuery("date", range.getEndYear(), Double.POSITIVE_INFINITY),BEGIN_DATE_WEIGHT);
      BoostQuery qEndDate = new BoostQuery(DoublePoint.newRangeQuery("date", Double.NEGATIVE_INFINITY, range.getEndYear()),END_DATE_WEIGHT);
      builder.add(qBeginDate,BooleanClause.Occur.SHOULD);
      builder.add(qEndDate,BooleanClause.Occur.SHOULD);
    }
  }


  /**
   * Add queries from the type field if detects any type pattern in the information need.
   * @param sentence  Sentence to query.
   * @param builder Builder of the main boolean query.
   */
  private void queryType(String sentence, BooleanQuery.Builder builder) {
    for(TypeDetector.AcademicWorkType type: TypeDetector.detectWorkType(sentence)){
      BoostQuery query = new BoostQuery(new TermQuery(new Term("type", type.getValue())), TYPE_WEIGHT);
      builder.add(query,BooleanClause.Occur.SHOULD);
    }
  }

  /**
   * Add queries from the description field if detects any location pattern in the information need.
   * @param sentence  Sentence to query.
   * @param builder Builder of the main boolean query.
   */
  private void queryLocations(String sentence, Analyzer analyzer, BooleanQuery.Builder builder) throws ParseException {
    String tokens[] = sentence.split(" ");
    for (String s : tokens) {
      s = Detector.normalize(s);
      if (Character.isUpperCase(s.charAt(0)) && (locationDetector.detect(s))) {
        BoostQuery qDescription = new BoostQuery(new QueryParser("description", analyzer).parse(s), LOCATION_WEIGHT);
        builder.add(qDescription, BooleanClause.Occur.SHOULD);
      }
    }
  }
  /**
   * Add queries from the description, creator and contributor fields if detects any name pattern in the information need.
   * @param sentence  Sentence to query.
   * @param builder Builder of the main boolean query.
   */
  private void queryNames(String sentence,Analyzer analyzer, BooleanQuery.Builder builder) throws ParseException {
    String tokens[] = sentence.split(" ");
    for(String s : tokens) {
      s = Detector.normalize(s);
      if (Character.isUpperCase(s.charAt(0)) && (nameDetector.detect(s))) {
        BoostQuery qContributor = new BoostQuery(new QueryParser("creator", analyzer).parse(s), NAME_CREATOR_WEIGHT);
        BoostQuery qCreator = new BoostQuery(new QueryParser("contributor", analyzer).parse(s), NAME_CONTRIBUTOR_WEIGHT);
        BoostQuery qDescription = new BoostQuery(new QueryParser("description", analyzer).parse(s), DESCRIPTION_NAME_WEIGHT);
        builder.add(qContributor, BooleanClause.Occur.SHOULD);
        builder.add(qCreator, BooleanClause.Occur.SHOULD);
        builder.add(qDescription, BooleanClause.Occur.SHOULD);
      }
    }
  }

  /**
   * Prints the results of the search.
   * @param searcher    Object to search with in the indexed files.
   * @param query       Query to search.
   * @throws IOException
   */
  public void showResults(IndexSearcher searcher, Query query) throws IOException {

    // Collect enough docs to show 5 pages
    TopDocs results = searcher.search(query, Integer.MAX_VALUE);
    ScoreDoc[] hits;

    int numTotalHits = Math.toIntExact(results.totalHits.value);
    hits = searcher.search(query, numTotalHits).scoreDocs;

      for (int i = 0; i < 10; i++) {         // output raw format
        Document doc = searcher.doc(hits[i].doc);
        String path = doc.get("path");
        String [] pathElements = path.split("/");
        String filename = pathElements[pathElements.length -1];
        System.out.println(filename + " score=" + hits[i].score);
        //System.out.println("score : " + searcher.explain(query, hits[i].doc));
      }
  }

  /**
   * Writes the results of the search into a file with the next format:
   * INFORMATION-IDENTIFIER\tRESULT-FILE-NAME.
   * @param searcher    Object to search with in the indexed files.
   * @param query       Query to search.
   * @throws IOException
   */
  private void writeResults(IndexSearcher searcher, BooleanQuery query, String infoNeedId) throws IOException {

    BufferedWriter out = new BufferedWriter(new FileWriter(outputPath, true));

    TopDocs results = searcher.search(query,Integer.MAX_VALUE);
    ScoreDoc[] hits;

    int numTotalHits = Math.toIntExact(results.totalHits.value);
    hits = results.scoreDocs;

    for (int i = 0; i < numTotalHits; i++) {
      Document doc = searcher.doc(hits[i].doc);
      String path = doc.get("path");
      String [] pathElements = path.split("/");
      String filename = pathElements[pathElements.length -1];
      out.write(infoNeedId + "\t" + filename + "\n");
    }
    out.close();
  }


  public static void main(String[] args) throws Exception {
    new SearchFiles(args);
  }
}