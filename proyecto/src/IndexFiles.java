
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Paths;
import java.util.Date;

/**
 * @author Martín Gascón 764429
 * @author Eduardo Ruiz  764539
 * Index all text files under a directory.
 */
public class IndexFiles {
  private enum FieldTypes {
    STRING_FIELD, TEXT_FIELD, LOCATION_FIELD
  }
  private IndexFiles(String args[]) {

    String usage = "IndexFiles"
            + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
            + "This indexes the documents in DOCS_PATH, creating a Lucene index"
            + "in INDEX_PATH that can be searched with SearchFiles";

    String indexPath = "index";
    String docsPath = null;
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final File docDir = new File(docsPath);
    if (!docDir.exists() || !docDir.canRead()) {
      System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }

    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = new CustomAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      }

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);
      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
              "\n with message: " + e.getMessage());
    }

  }





  /** Index all text files under a directory. */
  public static void main(String[] args) {
    new IndexFiles(args);
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   *
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param file The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(IndexWriter writer, File file)
          throws IOException {
    // do not try to index files that cannot be read
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {

        FileInputStream fis;
        try {
          fis = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
          // at least on windows, some temporary files raise this exception with an "access denied" message
          // checking if the file can be read doesn't help
          return;
        }

        try {

          // make a new, empty document
          Document doc = new Document();
          Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
          doc.add(pathField);


          org.w3c.dom.Document dc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);

          indexField(FieldTypes.TEXT_FIELD,	"title",	"dc:title",	dc, doc);
          indexField(FieldTypes.TEXT_FIELD,	"subject",	"dc:subject",	dc, doc);
          indexField(FieldTypes.TEXT_FIELD,	"description",	"dc:description",	dc, doc);
          indexField(FieldTypes.TEXT_FIELD,	"contributor",	"dc:contributor",	dc, doc);
          indexField(FieldTypes.TEXT_FIELD,	"creator",	"dc:creator",	dc, doc);
          indexField(FieldTypes.TEXT_FIELD,	"date",	"dc:date",	dc, doc);
          indexField(FieldTypes.STRING_FIELD,	"type",	"dc:type",	dc, doc);
          indexField(FieldTypes.TEXT_FIELD,	"publisher",	"dc:publisher",	dc, doc);


          if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            System.out.println("adding " + file);
            writer.addDocument(doc);
          } else {
            // Existing index (an old copy of this document may have been indexed) so
            // we use updateDocument instead to replace the old one matching the exact
            // path, if present:
            System.out.println("updating " + file);
            writer.updateDocument(new Term("path", file.getPath()), doc);
          }

        } catch (ParserConfigurationException | SAXException e) {
          e.printStackTrace();
        }  finally {
          fis.close();
        }
      }
    }
  }

  /**
   * Generic index method which add the different fields to the document.
   * @param fieldType Type of the field to index.
   * @param name  Name of the index field.
   * @param tag Tag of the source document field.
   * @param dc  DOM document of the source file to index.
   * @param doc Indexing document.
   */
  private static void indexField(FieldTypes fieldType,  String name, String tag, org.w3c.dom.Document dc, Document doc){
    NodeList nl = dc.getElementsByTagName(tag);
    if(nl.item(0) == null)  return;
    switch(fieldType){
      case TEXT_FIELD:
        doc.add(new TextField(name, new BufferedReader(new StringReader(nl.item(0).getTextContent()))));
        break;
      case STRING_FIELD:
        doc.add(new StringField(name, nl.item(0).getTextContent(), Field.Store.YES));
        break;
      default:
    }
  }
}