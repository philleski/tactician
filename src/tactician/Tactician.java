package tactician;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import tactician.Board;
import tactician.Brain;

/**
 * <p>
 * This class is the entry point to the chess engine. It implements part of the Universal Chess
 * Interface in order to accept commands from a chess GUI.
 * 
 * <p>
 * A chess program is typically composed of two parts, a GUI and an engine. This package only
 * implements an engine, so in order to play chess comfortably we recommend that it be connected to
 * a chess GUI. See the README file for examples. It is still possible to play against the engine
 * on the command line, though not as easy.
 * 
 * <p>
 * The GUI is responsible for displaying a graphical representation of the board and pieces, for
 * managing time and enforcing time limits, and for enforcing move legality. The engine is
 * responsible for playing a move based on a board position within certain time limits. Typically
 * the GUI is also responsible for the opening book, selecting randomly from the most common
 * openings in order to add more variety to the games.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Chess_engine#Interface_protocol"> Universal Chess
 *      Interface</a>
 * @author Phil Leszczynski
 */
public class Tactician {
  /** Deletes the log file. */
  public static void logClear() {
    try {
      new File(logFilename).delete();
    } catch (Exception e) {
    }
  }

  /**
   * Appends a line to the log file.
   * 
   * @param line the text to append to the log file, followed by a newline
   */
  public static void log(String line) {
    try {
      FileWriter writer = new FileWriter(logFilename, true);
      writer.write(line + "\n");
      writer.close();
    } catch (IOException e) {
    }
  }

  /**
   * Responds to a command from the chess GUI. This just prints the line to standard out and also
   * appends the line to the log file.
   * 
   * @param line the response to the GUI command
   */
  public static void respond(String line) {
    log("> " + line);
    System.out.println(line);
  }

  /**
   * Logs the principal variation of a given move.
   * 
   * @param move the move from where the principal variation begins
   */
  public static void logPrincipalVariation(Move move) {
    ArrayList<Move> principalVariation = brain.getPrincipalVariation(board, move);
    ArrayList<String> movesAlgebraic = new ArrayList<String>();
    for (Move pvMove : principalVariation) {
      movesAlgebraic.add(AlgebraicNotation.moveToAlgebraic(board, pvMove));
      board.move(pvMove);
    }
    log("PV: " + movesAlgebraic);
  }

  /**
   * Interprets a command as specified by the Universal Chess Interface and respond to it. Logs the
   * command as well. If the command is not understood, do nothing and wait for the next command.
   * 
   * @param line the command sent in from the chess GUI
   */
  public static void interpretUCICommand(String line) {
    log("< " + line);
    if (line.equals("uci")) {
      respond("id name Tactician");
      respond("id author Phil Leszczynski");
      respond("uciok");
    } else if (line.equals("isready")) {
      respond("readyok");
    } else if (line.startsWith("position fen ")) {
      String fenstring = line.substring(13);
      log(fenstring);
      if (board.enPassantTarget != 0) {
        int enPassantTargetIndex = new Bitboard(board.enPassantTarget).numEmptyStartingSquares();
        log("EP Target: " + new Square(enPassantTargetIndex).getName());
      } else {
        log("EP Target: None");
      }
      board.setPositionFenstring(fenstring);
      log(board.toString());
    } else if (line.startsWith("go ")) {
      Move move = brain.getMove(board);
      String moveLongAlgebraic = move.toString();
      logPrincipalVariation(move);
      respond("bestmove " + moveLongAlgebraic);
    }
  }

  /**
   * Listens to UCI commands, typically from a chess GUI, and responds to them.
   * 
   * @param args the standard main function command-line arguments, not used here
   */
  public static void main(String[] args) {
    logClear();
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    String line = "";
    while (true) {
      try {
        line = reader.readLine();
      } catch (IOException e) {
      }
      interpretUCICommand(line);
    }
  }

  /** The filename where we log commands and other details about the position for debugging */
  private static String logFilename = "~/chess.log";

  private static Board board = new Board();
  private static Brain brain = new Brain();
}
