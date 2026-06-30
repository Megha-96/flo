# Flo

## Technical Assessment

* sample input file : https://github.com/Megha-96/flo/blob/main/input.csv

* sample output file : https://github.com/Megha-96/flo/blob/main/output.sql
  
* Core files : https://github.com/Megha-96/flo/tree/main/src/main/java/com/floenergy/core
  - MeterDataReader
  - Nem12Parser/ParserFactory
  - Sql Writer
  - ConsumptionValueValidator
  
* Entry point/Main file : https://github.com/Megha-96/flo/blob/main/src/main/java/com/floenergy/Main.java
* More details on core component : https://github.com/Megha-96/flo/blob/main/README.md#core-components


  
## Assessment Questions

### What is the rationale for the technologies you have decided to use?

I chose Java for this implementation because it provides strong standard library support for file handling, date/time processing, validation, and object-oriented design.

The application uses Maven as the build tool because it provides a simple and standard way to manage project structure, dependencies, builds, and tests. It also makes the project easy to run and verify using common commands such as:

```bash
mvn clean test
mvn clean package
```

For file processing, I used `BufferedReader` and `BufferedWriter` from the Java standard library. This was an intentional choice because the input files can be very large. Reading the file line by line avoids loading the full NEM12 file into memory, making the application more scalable for large files.

JUnit 5 is used for testing because it is a widely used testing framework in Java and allows the parsing, validation, timestamp calculation, and SQL writing logic to be tested independently.

I avoided using Spring Boot or heavier frameworks to keep the application lightweight and focused on the core parsing and SQL generation functionality.

---

### What would you have done differently if you had more time?

#### Current Assumptions

The current implementation is based on the following assumptions:

* Fields must not include leading or trailing spaces.
* A null value is not allowed in the `IntervalValue` field of the NEM12 file.
* A comma is required between all fields, even if the field is null.
* Commas are not permitted in any data field.
* Records are expected to follow the correct blocking order:

```text
100 -> 200 -> 300 -> 400 -> 500 -> 900
```

* A `300` interval record must appear after a corresponding `200` NMI data detail record.
* The current implementation validates the fields needed to generate meter reading SQL, such as:

  * NMI
  * Interval length
  * Interval date
  * Consumption values
* All consumption values are currently assumed to use `kWh` as the unit of measure.

---

#### Further Improvements

##### Full NEM12 Specification Validation and zip file input support

With more time, I would extend the parser to validate the full NEM12 specification more strictly.
To support .zip files as allowed by the MDFF specification.
This would include:

* Mandatory field validation for `100`, `200`, `300`, `400`, `500`, and `900` records
* NMI value validation, including prefix validation
* Correct record sequencing validation
* Unit of measure validation
* Conversion of consumption values into one uniform unit of measure
* Validation of quality methods and reason codes

---

##### Spring Boot Application with Direct Database Ingestion

The current requirement is to generate SQL insert statements.

For a production-grade version, I would introduce an API-based ingestion layer to upload MeterData files and persist parsed meter readings directly into configurable data sources such as PostgreSQL or MySQL using an ORM framework like JPA/Hibernate.

This could include:

* A REST API to upload MeterData files
* Database integration with PostgreSQL or MySQL
* ORM-based persistence using JPA/Hibernate
* JDBC batch inserts for higher throughput
* Transactional database writes
* Retry handling for transient database failures

This would reduce output file size and improve ingestion performance for very large files.

---

##### Configurable Output Format

The current implementation generates SQL output.

In the future, the output format could be made configurable.

Supported output modes could include:

```text
SQL_INSERT
BATCHED_SQL_INSERT
CSV
JSON
DIRECT_DB_WRITE
```

This would make the application more flexible and easier to integrate with different downstream systems.

---

##### Larger Batch Size Control

The current batched insert approach generates one batched insert per `300` record.

A future optimization could support configurable batch sizes, for example:

```text
500 rows per insert
1000 rows per insert
5000 rows per insert
```

This would give more control over output size, SQL execution performance, and database compatibility.

### What is the rationale for the design choices that you have made? 

The design focuses on simplicity, testability, extensibility, and large-file handling.

#### Streaming File Processing

The application reads the input file line by line instead of loading the full file into memory.

This is important because NEM12 files can be very large. Streaming keeps memory usage low and makes the application suitable for multi-GB files.

#### Separation of Responsibilities

The code is split into focused components:

* `Main` handles application startup and command-line arguments.
* `MeterDataReader` coordinates file reading and record processing.
* `MDParserFactory` selects the correct parser based on the file type.
* `Nem12Parser` contains NEM12-specific parsing logic.
* `ConsumptionValueValidator` validates interval consumption values.
* `IntervalTimestampCalculator` calculates interval timestamps.
* `SqlWriter` writes SQL output.

This separation keeps each class easier to understand, test, and modify.

#### Parser Abstraction

The parser is accessed through an `MDParser` interface and selected using `MDParserFactory`.

Although the current implementation supports NEM12, this design allows additional parser implementations to be added later without changing the file reading flow.

#### Maintaining Current NMI Context

NEM12 files contain `200` records that define the NMI and interval length, followed by `300` records containing interval values.

The reader keeps track of the current `200` record context so that each following `300` record can be correctly associated with the right NMI.

#### Interval Timestamp Calculation

The timestamp calculation is kept in a separate utility class because it is a core rule of the NEM12 format.

This makes the logic reusable and easy to test independently.

For example, for a 30-minute interval:

```text
Interval 1  -> 00:30:00
Interval 2  -> 01:00:00
Interval 48 -> 00:00:00 on the next day
```

#### Consumption Value Validation

Consumption validation is separated from parsing so that the parser does not become responsible for all validation rules.

This keeps the parsing logic cleaner and allows validation rules to be tested independently.

The validator rejects invalid values such as:

* Blank values
* Non-numeric values
* Negative values
* Exponential notation

#### SQL Writer Abstraction

SQL generation is handled by `SqlWriter`, separate from parsing.

This makes it possible to change the output format later without affecting the parser.

The application supports batched insert generation because it reduces the number of SQL statements and is more efficient for larger files compared with generating one insert statement per interval reading.



## Project Overview

This project is a Java-based command-line application for parsing NEM12 meter data files and generating SQL insert statements for meter readings.

The application reads a NEM12 CSV file, parses NMI details and interval consumption records, calculates the correct timestamp for each interval reading, validates consumption values, and writes the parsed data into an output SQL file.

The generated SQL targets the following table structure:

```sql
meter_readings (
    nmi,
    timestamp,
    consumption
)
```

## Intent

The intent of this application is to convert NEM12 interval meter data into SQL statements that can be loaded into a database.

For each valid interval reading in the input file, the application generates an insert statement containing:

* NMI
* Interval timestamp
* Consumption value

The application supports both standard insert generation and batched insert generation.

## High-Level Flow

```text
Input NEM12 CSV file
        ↓
Read file line by line
        ↓
Identify record type
        ↓
Parse 200 NMI detail records
        ↓
Parse 300 interval data records
        ↓
Validate consumption values
        ↓
Calculate interval timestamps
        ↓
Generate SQL insert statements
        ↓
Write output.sql
```

## Core Components

### Main

The `Main` class is the entry point of the application.

It is responsible for:

* Reading command-line arguments
* Resolving input and output file paths
* Initializing the meter data reader
* Handling top-level application errors

#### Example Usage

```bash
java -jar target/nem12-parser-1.0-SNAPSHOT.jar input.csv output.sql
```

If no output path is provided, the application can use a default output file such as:

```text
output.sql
```

### MeterDataReader

`MeterDataReader` is responsible for reading the input file and coordinating the parsing flow.

It reads the file using a `BufferedReader`, which allows the application to process large files without loading the entire file into memory.

It processes records based on their record indicator:

```text
100 -> Header record
200 -> NMI data detail record
300 -> Interval data record
400 -> Event record, ignored for current requirement
500 -> B2B detail record, ignored for current requirement
900 -> End of file record
```

The reader keeps track of the current `200` record context so that subsequent `300` interval records can be associated with the correct NMI.

### MDParserFactory

`MDParserFactory` is responsible for returning the correct parser based on the meter data file type.

Currently, the application supports:

```text
NEM12
```

This keeps the design extensible. If support for another meter data format is required later, a new parser implementation can be added without changing the main reading flow.

### Nem12Parser

`Nem12Parser` contains the NEM12-specific parsing logic.

It is responsible for:

* Extracting record indicators
* Parsing `200` records
* Validating NMI and interval length
* Parsing `300` interval records
* Generating interval-level meter reading records
* Ensuring the number of interval values matches the interval length

For example, based on the interval length:

```text
30-minute interval -> 48 readings per day
15-minute interval -> 96 readings per day
5-minute interval  -> 288 readings per day
```

### IntervalTimestampCalculator

`IntervalTimestampCalculator` calculates the timestamp for each interval value.

For a NEM12 `300` record, the interval date represents the trading date, and each interval value represents the reading at the end of that interval.

For example, with a 30-minute interval length:

```text
Interval 1  -> 00:30:00
Interval 2  -> 01:00:00
Interval 48 -> 00:00:00 on the next day
```

### ConsumptionValueValidator

`ConsumptionValueValidator` validates interval consumption values before they are written to SQL.

It checks for invalid values such as:

* Blank values
* Non-numeric values
* Negative values
* Exponential notation

Only valid consumption values are converted into meter reading records.

### SqlWriter

`SqlWriter` writes parsed meter readings into an output SQL file.

It supports two output styles:

#### Standard Insert Mode

```sql
INSERT INTO meter_readings ("nmi", "timestamp", "consumption") VALUES ('NEM1201009', '2005-03-01 00:30:00', 0);
INSERT INTO meter_readings ("nmi", "timestamp", "consumption") VALUES ('NEM1201009', '2005-03-01 01:00:00', 0.461);
```

#### Batched Insert Mode

```sql
INSERT INTO meter_readings ("nmi", "timestamp", "consumption") VALUES
('NEM1201009', '2005-03-01 00:30:00', 0),
('NEM1201009', '2005-03-01 01:00:00', 0.461);
```

Batched insert mode reduces the number of SQL statements generated and is more efficient for large input files.

## Large File Handling

The application is designed to handle large NEM12 files efficiently.

It uses streaming file processing:

```text
Read one line
Process that line
Write output incrementally
Move to the next line
```

This avoids loading the entire input file into memory.

The application uses:

* `BufferedReader`
* `BufferedWriter`

This makes it suitable for processing large files, including multi-GB input files, as long as the input file is provided as an external file path.

Large files should not be placed inside:

```text
src/main/resources
```

Instead, they should be passed as runtime input:

```bash
java -jar target/nem12-parser-1.0-SNAPSHOT.jar /path/to/input.csv /path/to/output.sql
```

## Error Handling

The application handles errors at different levels.

Examples of validation and error handling include:

* Invalid header records
* Unsupported parser types
* Missing `200` record before a `300` record
* Invalid interval length
* Insufficient consumption values
* Invalid consumption values
* File read/write errors

For recoverable record-level errors, the application can log the issue and continue processing subsequent records.

For critical errors, such as missing input files or invalid headers, the application fails fast.

## Testing

### Testing with Default Input File

You can test the application using a sample NEM12 input file.

Default input file is placed here:

```text
flo/input.csv
```

Build the application:

```bash
mvn clean package
```

Run the JAR:

```bash
java -jar target/nem12-parser-1.0-SNAPSHOT.jar
```

This should generate:

```text
output.sql
```

The output file should contain SQL insert statements for the parsed meter readings.

### Testing with Default Output Path

If only the input file path is provided:

```bash
java -jar target/nem12-parser-1.0-SNAPSHOT.jar input.csv
```

The application should generate the output file using the default path:

```text
output.sql
```

### Testing with Absolute Paths

You can also pass absolute paths:

```bash
java -jar target/nem12-parser-1.0-SNAPSHOT.jar /Users/meghamadan/IdeaProjects/flo/input.csv /Users/meghamadan/IdeaProjects/flo/output.sql
```

This is useful when testing files outside the project directory.
