# Single read CSV processor
Using Java, read a CSV data file while processing the contents at the same time.


* Maven is used as dependency management and build tool
* Eclipse project is included with both run and debug configurations
* Java 8 is preferred to make use of lambda expressions
* Apache Commons CSV is used for reading Comma Separated Value (CSV) format
* System properties used to read the CSV file, will work in both POSIX and Windows
* Log4j logging system used to log errors
* Immutable Map to store data
* Tests provided through a single test runner


# How to run

* Clone this repository
* To **build** the project, go to folder CSV-processor in a terminal or command prompt and type `mvn install`
* To **run** the project, type `java -jar target/wage-calculator-1.0.jar`
* To build **eclipse project** type `mvn eclipse:clean eclipse:eclipse`
