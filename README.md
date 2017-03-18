# Tactician

An AI program that plays chess by reading in moves from the command line.

## Compiling

In Eclipse, go to File -> Export. Then in the Java directory, select Runnable Jar File. Note the location of the exported file.

## Running

Since this is just a chess engine without a GUI, you'll need to install a GUI in order to play. I would recommend ScidvsMac.

In ScidvsMac go to Tools -> Analysis Engines. Create an engine with the following configuration:

Name: chess-engine

Command: `/usr/bin/java`

Directory: .

Parameters: `-Xms512M -Xmx1524M -jar ~/tactician.jar`

The two parameter options give the program extra memory for the transposition tables.

Once the engine is configured you can play a game in ScidvsMac by going to Play -> Computer - UCI Engine. If you'd like to play as black they have an icon for that right above the board.

## Testing

The JUnit tests are located in the Test subfolder. They should all pass before committing to the master branch. Ideally the time to run performance tests in TestPerformance.java should be comparable to the previous build, unless there is a good enough reason for the lower performance.

## Style

We use the Google Java style guide for this project: https://google.github.io/styleguide/javaguide.html
