# query-annotator-stub

This project contains a stub for the implementation and benchmarking of an entity annotator on queries. The project is mavenized. There is also an example for calling the Bing Api, in case you need it.

##How you should proceed
We suggest to:
- Fork this project on Github (you need a github account)
- Develop your annotator by editing the provided java files
- Run the benchmark to see how your annotator performs.

## Included classes and POM
### POM
File [pom.xml](pom.xml) defines a Maven project. It includes two dependencies: **bat-framework** and **bing-api-java**. You need the [BAT-framework](http://www.github.com/marcocor/bat-framework) to benchmark your annotation system, and the [Bing java API](http://www.github.com/marcocor/bing-api-java) to access the Bing API (in case your project is built on top of Bing).

### Java classes
- [FakeAnnotator](src/main/java/annotatorstub/annotator/FakeAnnotator.java) is the definition of a new annotator. We suggest to edit only the functions **solveSa2W** and **getName**. The first one implements the actual query entity linking algorithm. It does it in a very naive way: it takes the first word of the query and, in case there is a Wikipedia page with that title, it links the word to that page.

- [AnnotatorMain](src/main/java/annotatorstub/main/AnnotatorMain.java) is an example main that asks the annotator defined in the class above to annotate the query **strawberry fields forever**. Since there is a Wikipedia page called [Strawberry](http://en.wikipedia.org/wiki/Strawberry), the annotator links the mention **strawberry** to the entity [Strawberry](http://en.wikipedia.org/wiki/Strawberry). You can try and change the query to see how it works.

- [BenchmarkMain](src/main/java/annotatorstub/main/BenchmarkMain.java) launches the evaluation of our system against the GERDAQ dataset (development portion) and prints the results. C2W results refer to the capacity of the annotator to spot correct entities, while A2W results reflects its capacity to spot correct mention-entity pairs (a.k.a annotations). By running the program, you will find out that our annotator achieves 17.2% in macro-F1, which is quite poor, nonetheless we find 50 True positives (correct annotations). Right before printing the final results, the program prints, for each query of the evaluation dataset, the entities that the annotator has found, and those that it should have found (the gold standard).

- [BingSearchMain](src/main/java/annotatorstub/main/BingSearchMain.java) contains an example usage of the Bing search API. To run it, you must insert a valid key to the Bing API, which can be obtained for free (up to 5000 queries per month) [here](http://datamarket.azure.com/dataset/bing/search).

##Tips
- For training your annotator, you can access the training portion of the GERDAQ dataset. It is divided in two parts: trainingA and trainingB. The BAT-Framework provides all methods to generate datasets in class **DatasetBuilder**.
- The BAT-Framework has a bunch of methods that you might find useful. Have a look at classes [DumpData](https://github.com/marcocor/bat-framework/blob/master/src/main/java/it/unipi/di/acube/batframework/utils/DumpData.java) and [DumpResults](https://github.com/marcocor/bat-framework/blob/master/src/main/java/it/unipi/di/acube/batframework/utils/DumpResults.java).
 

## Issues
- For any issue or feature request, open an [issue](https://github.com/marcocor/query-annotator-stub/issues) on github.
