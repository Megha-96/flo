# flo
Technical Assessment
Overview
## Project Overview
This project is a Java-based command-line application for parsing NEM12 meter data files and generating SQL insert statements for meter readings.

The application reads a NEM12 CSV file, parses NMI details and interval consumption records, calculates the correct timestamp for each interval reading, validates consumption values, and writes the parsed data into an output SQL file.

The generated SQL targets the following table structure:

meter_readings (
    nmi,
    timestamp,
    consumption
)

### Intent

The intent of this application is to convert NEM12 interval meter data into SQL statements that can be loaded into a database.

For each valid interval reading in the input file, the application generates an insert statement containing:

NMI
Interval timestamp
Consumption value

The application supports both standard insert generation and batched insert generation.


### High-Level Flow

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

### Main Components
1. Main

The Main class is the entry point of the application.

It is responsible for:

Reading command-line arguments
Resolving input and output file paths
Initializing the meter data reader
Handling top-level application errors

Example usage:

java -jar target/nem12-parser-1.0-SNAPSHOT.jar input.csv output.sql

If no output path is provided, the application can use a default output file such as:

output.sql
2. MeterDataReader

MeterDataReader is responsible for reading the input file and coordinating the parsing flow.

It reads the file using a BufferedReader, which allows the application to process large files without loading the entire file into memory.

It processes records based on their record indicator:

100 -> Header record
200 -> NMI data detail record
300 -> Interval data record
400 -> Event record, ignored for current requirement
500 -> B2B detail record, ignored for current requirement
900 -> End of file record

The reader keeps track of the current 200 record context so that subsequent 300 interval records can be associated with the correct NMI.

3. MDParserFactory

MDParserFactory is responsible for returning the correct parser based on the meter data file type.

Currently, the application supports:

NEM12

This keeps the design extensible. If support for another meter data format is required later, a new parser implementation can be added without changing the main reading flow.

4. Nem12Parser

Nem12Parser contains the NEM12-specific parsing logic.

It is responsible for:

Extracting record indicators
Parsing 200 records
Validating NMI and interval length
Parsing 300 interval records
Generating interval-level meter reading records
Ensuring the number of interval values matches the interval length

For example, based on the interval length:

30-minute interval -> 48 readings per day
15-minute interval -> 96 readings per day
5-minute interval  -> 288 readings per day
5. IntervalTimestampCalculator

IntervalTimestampCalculator calculates the timestamp for each interval value.

For a NEM12 300 record, the interval date represents the trading date, and each interval value represents the reading at the end of that interval.

For example, with a 30-minute interval length:

Interval 1  -> 00:30:00
Interval 2  -> 01:00:00
Interval 48 -> 00:00:00 on the next day
6. ConsumptionValueValidator

ConsumptionValueValidator validates interval consumption values before they are written to SQL.

It checks for invalid values such as:

Blank values
Non-numeric values
Negative values
Exponential notation

Only valid consumption values are converted into meter reading records.

7. SqlWriter

SqlWriter writes parsed meter readings into an output SQL file.

It supports two output styles:

Standard Insert Mode
INSERT INTO meter_readings ("nmi", "timestamp", "consumption") VALUES ('NEM1201009', '2005-03-01 00:30:00', 0);
INSERT INTO meter_readings ("nmi", "timestamp", "consumption") VALUES ('NEM1201009', '2005-03-01 01:00:00', 0.461);
Batched Insert Mode
INSERT INTO meter_readings ("nmi", "timestamp", "consumption") VALUES
('NEM1201009', '2005-03-01 00:30:00', 0),
('NEM1201009', '2005-03-01 01:00:00', 0.461);

Batched insert mode reduces the number of SQL statements generated and is more efficient for large input files.

Large File Handling

The application is designed to handle large NEM12 files efficiently.

It uses streaming file processing:

Read one line
Process that line
Write output incrementally
Move to the next line

This avoids loading the entire input file into memory.

The application uses:

BufferedReader
BufferedWriter

This makes it suitable for processing large files, including multi-GB input files, as long as the input file is provided as an external file path.

Large files should not be placed inside:

src/main/resources

Instead, they should be passed as runtime input:

java -jar target/nem12-parser-1.0-SNAPSHOT.jar /path/to/input.csv /path/to/output.sql
Error Handling

The application handles errors at different levels.

Examples of validation and error handling include:

Invalid header records
Unsupported parser types
Missing 200 record before a 300 record
Invalid interval length
Insufficient consumption values
Invalid consumption values
File read/write errors

For recoverable record-level errors, the application can log the issue and continue processing subsequent records.

For critical errors, such as missing input files or invalid headers, the application fails fast.

## Testing

### Testing with Default input file
You can test the application using a sample NEM12 input file.

Default input file is placed here : flo/input.csv

Then build the application:

mvn clean package

Run the JAR:

java -jar target/nem12-parser-1.0-SNAPSHOT.jar 

This should generate:

output.sql

The output file should contain SQL insert statements for the parsed meter readings.

### Testing with Default Output Path

If only the input file path is provided:

java -jar target/nem12-parser-1.0-SNAPSHOT.jar input.csv

the application should generate the output file using the default path:

output.sql

### Testing with Absolute Paths

You can also pass absolute paths:

java -jar target/nem12-parser-1.0-SNAPSHOT.jar /Users/meghamadan/IdeaProjects/flo/input.csv /Users/meghamadan/IdeaProjects/flo/output.sql

This is useful when testing files outside the project directory.
