package annotatorstub.main.old;

import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

import java.io.IOException;
import java.util.HashSet;

import annotatorstub.annotator.FakeAnnotator;

public class AnnotatorMain {

	public static void main(String[] args) throws IOException {
		FakeAnnotator ann = new FakeAnnotator();
		String query = "strawberry fields forever";
		HashSet<Annotation> annotations = ann.solveA2W(query);
		for (Annotation a : annotations) {
			int wid = a.getConcept();
			String title = WikipediaApiInterface.api().getTitlebyId(a.getConcept());
			System.out.printf("found annotation: %s -> %s (id %d) link: http://en.wikipedia.org/wiki/index.html?curid=%d%n", query.substring(a.getPosition(), a.getPosition() + a.getLength()), title, wid, wid);
		}
		WikipediaApiInterface.api().flush();
	}
}
