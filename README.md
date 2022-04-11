# TSSM Dataset

## Creation Steps

### (1) List of Java projects
Mining the GitHub repository is a time-consuming activity because it has more than 8 million projects. Therefore, we used the 147,991 Java projects with more than five stars listed by [Loriot et al.](https://arxiv.org/abs/1904.01754) (2020) and [Durieux et al.](https://arxiv.org/abs/2103.09672) (2021). The list of projects is available at ```project_list.txt```. 

### (2) Filtering projects from Github
We established the following criteria to select projects that match the requirements of data extraction tools:
* **Open-source projects.** We limit our search to projects with a declared license
compliant with OSI (Open-Source Initiative) or FSF (Free Software Foundation) licensing;
* **Non-forked projects.** We removed the forked projects because they contain excerpts of code similar to the original ones, which return similar values in the data extraction, biasing the results.
* **Projects that use Java as the primary programming language.** Besides the initial list contains projects that use Java as the primary programming language, the projects evolved since the GitHub mining. Therefore, we checked whether the projects continue using Java as a primary language. 
* **Projects that contain tests matching the production classes.** We selected the projects that have test classes. Even more, as different packages can have classes with the same name, we added a constraint that the test classes and production classes should be at the same hierarchical level in the project. It helps the static analysis to match the test classes with their respective production classes. 
The list of selected projects is available at ```selected_projects.txt```. 


### (3) Data extraction
We developed the ```JAVA``` to collect the following data of the projects:
* **Test Smells.** We used the [JNose Test](https://jnosetest.github.io/) to collect data of test smells in the test code. 
* **Structural metrics.** We used the [CK Metrics](https://github.com/mauricioaniche/ck) to collect structural metrics from the test and production code.
* **Metadata from GitHub.** We used the [GHRepository](https://github-api.kohsuke.org/apidocs/index.html) to collect the metadata of the projects sucessfully executed by JNose Test and CK Metrics. 

Next, we developed the ```MERGE``` to merge the test smells and metrics. We analyzed the collected data at class and method levels to establish a traceability link between the JNose Test and CK Metrics tools. It is important to notice that not all production classes of a project match with their respective test class, and the same occurs at the method level. We followed the JUnit naming convention of either pre-pending or appending the word ``Test`` to the name of the production class at the same level at the package hierarchy. For example, a production class in the package ``/src/java/example/``is called ``ExampleName.java``, so its test class should be in the package ``/src/test/example`` and named as ``ExampleNameTest.java`` or ``TestExampleName.java``.

We made the files containing the data on test smells and metrics available in the folder ```JSet```. It is structured as follows:

```
JSet
│
├── Test Smells
│   ├── classSmells.csv: contains data of 18 test smells at class level
│   ├── methodSmells.csv: contains data of 18 test smells at method level
├── Metrics: 
│   ├── testClass.csv: contains data of 44 structural metrics at test class level
│   ├── testMethod.csv: contains data of 28 structural metrics at test method level 
│   ├── productionClass.csv: contains data of 44 structural metrics at production class level
│   ├── productionMethod.csv: contains data of 28 structural metrics at production method level 
├── Test smells and metrics:
│   ├── mergeTestClass.csv: contains data of 44 structural metrics and 18 test smells at test class level
│   ├── mergeTestMethod.csv: contains data of 28 structural metrics and 18 test smells at test method level
│   ├── mergeProductionClass.csv: contains data of 44 structural metrics and 18 test smells at production class level
│   ├── mergeProductionMethod.csv: contains data of 28 structural metrics and 18 test smells at production method level
|
```

## Download and Run

## Mining GitHub 

Prerequisites:
 - JDK 1.8 
 - Maven 3 

The JNose Test requires the [jnose-core](https://github.com/tassiovirginio/jnose-core) dependency. Install the dependecy following the steps: 

```shell
git clone git@github.com:arieslab/jnose-core.git
cd jnose-core
mvn install
```
Clone the project to generate the dataset using the following command:

```shell
git clone git@github.com:luana-martins/Dataset_TestSmells_Generator.git
```

Open the project in the IDE as a Maven project (we use the IntelliJ), configure and run the class ```Main.java``` with the information:
* **ghKey** receives a personal access token from GitHub. [Generating a ghKey](https://github.com/settings/tokens).
* **startNumberList** receives a initial lineID of a project from the ```project_list.txt``` to start the data collection. 
* **endNumberList** receives a final lineID of a project from the ```project_list.txt``` to start the data collection. 

## Merging Test smells and Metrics

Prerequisites:
 - Python 3

Open the ```scripts``` folder and execute the command:
```shell
python3 mergedata.py
```
