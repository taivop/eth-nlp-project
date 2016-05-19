package annotatorstub.main;

import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.datasetPlugins.YahooWebscopeL24Dataset;
import it.unipi.di.acube.batframework.problems.A2WDataset;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.HashSet;

public class WebScopeXMLProcessing {

    public static A2WDataset getWebscope() {
        return getWebscope("../data/yahoo-webscope/ydata-search-query-log-to-entities-v1_0.xml");
    }

    public static A2WDataset getWebscope(String fileName) {
        try {
            return new YahooWebscopeL24Dataset(fileName);
        }
        catch(IOException | ParserConfigurationException | XPathExpressionException | SAXException e) {
            throw new RuntimeException("Could not load Yahoo! Webscope dataset from: " +
                fileName, e);
        }

    }

    public static void main(String[] args) {
        A2WDataset bonusData = getWebscope();
        System.out.println("Loaded WebScope data: " + bonusData.getSize() + " data points");


        int limit = Math.min(5, bonusData.getSize());
        for (int i = 0; i < limit; i++) {
            System.out.println();
            System.out.println(bonusData.getTextInstanceList().get(i));
            System.out.println(bonusData.getA2WGoldStandardList().get(i).size());
            for (Annotation annotation : bonusData.getA2WGoldStandardList().get(i)) {
                System.out.println("\t- " + annotation.getConcept());
            }
        }
    }
}
