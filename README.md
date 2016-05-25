# An Improvement of SMAPH-S for Entity Linking of Web Queries
Abstract:

SMAPH-S is a precursor of [SMAPH-2](http://www2016.net/proceedings/proceedings/p567.pdf), a state-of-the-art system for joint entity mention detection and linking in web queries. Both systems use a piggyback approach to annotate queries. A set of candidate entities is drawn directly from Bing search results or annotations of Bing snippets and therefore performance depends heavily on the accuracy of Bing itself. Our system improves on SMAPH-S by systematically detecting queries which produce uninformative Bing results and rewrites them to extract better candidate entities. To this end, we split query strings into smaller chunks based on their linking probability. We also improve the way mention candidates are generated so that the system is able to handle noisy inputs as they are very common in web queries. Finally, we report the results of experimenting with different regressors in the pruning phase, such as Probabilistic Logistic Regression and AdaBoost.

The [piggyback paper](http://www2016.net/proceedings/proceedings/p567.pdf) contains additional details.


This project is based on [marcocor](https://github.com/marcocor)'s [query annotator stub](https://github.com/marcocor/query-annotator-stub). The project is mavenized.

## Dependencies
- Python with [scikit-learn](http://scikit-learn.org/) and [Flask](http://flask.pocoo.org/).
  - The pruner is written in Python using scikit-learn and relies on Flask to expose an API that is started and called from the Java pipeline.
- Scala
  - We use Scala to generate the dataset for training the pruner.

## Running
- Make sure you have all dependencies installed.
- Fill in your Bing API key in [config.properties](src/main/resources/config.properties).
- To benchmark our annotator, run [BenchmarkMain](src/main/java/annotatorstub/main/BenchmarkMain.java).

## Included classes and POM
### POM
File [pom.xml](pom.xml) defines a Maven project. It includes two dependencies: **bat-framework** and **bing-api-java**. You need the [BAT-framework](http://www.github.com/marcocor/bat-framework) to benchmark your annotation system, and the [Bing java API](http://www.github.com/marcocor/bing-api-java) to access the Bing API (in case your project is built on top of Bing).

## Important classes
- [SmaphSAnnotator](src/main/java/annotatorstub/annotator/smaph/SmaphSAnnotator.java) contains the improved SMAPH-S annotator we implemented.
