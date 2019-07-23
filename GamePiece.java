/*
 * Extra Credit done:
 * Enhanced graphics: gradient coloring
 * Step counter: increments whenever powerStation is moved or wire is rotated
 * Time elapsed: duration of game
 * Horizontal bias: Construct wires with a bias for the horizontal direction
 * Restart: allow the player to restart the game when "r" is pressed
 * Generate a new board: allows the player to generate a new board 
 * without randomization by pressing "g"
 * Leaderboard: shows the top 5 scores of all time achieved on this game. saves between sessions 
 */



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import javalib.worldimages.*;


class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  double radius;
  Random rand;
  int steps;
  Instant startTime;
  Leaderboard leaderboard;

  LightEmAll(int width, int height, Random rand) {
    this.width = width;
    this.height = height;
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.powerRow = 0;
    this.powerCol = 0;
    this.rand = rand;
    this.steps = 0;
    this.startTime = Instant.now();
    this.leaderboard = new Leaderboard();

    this.generateBoard();
    this.randomizeBoard();
    this.updateBoard();
  }
  
  LightEmAll(int width, int height) {
    this(width, height, new Random());
  }
  
  //generates the board
  void generateBoard() {
    
    this.powerCol = 0;
    this.powerRow = 0;
    this.board = new ArrayList<ArrayList<GamePiece>>();
    
    ArrayList<Edge> edges = new ArrayList<Edge>();
    for (int x = 0; x < width; x++) {
      this.board.add(new ArrayList<GamePiece>());
      for (int y = 0; y < height; y++) {
        this.board.get(x).add(new GamePiece(x, y, false, 0));
        if (x > 0) {
          GamePiece temp = this.board.get(x - 1).get(y);
          edges.add(new Edge(this.board.get(x).get(y), temp , 
              this.rand.nextInt(Math.min(this.width, this.height))));
        }
        if (y > 0) {
          GamePiece temp = this.board.get(x).get(y - 1);
          edges.add(new Edge(this.board.get(x).get(y), temp, 
              this.rand.nextInt(this.width * this.height)));
        }
      }
    }
    this.board.get(this.powerRow).get(this.powerCol).powerStation = true;
    Collections.sort(edges, new AscendingWeightSort());
    HashMap<GamePiece, GamePiece> mst = new HashMap<GamePiece, GamePiece>();
    
    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece gp : row) {
        mst.put(gp, gp);
      }
    }
    ArrayList<Edge> inTree = new ArrayList<Edge>();
    
    for (Edge e : edges) {
      if (!this.find(mst, e.gp1).equals(this.find(mst, e.gp2))) {
        inTree.add(e);
        mst.put(this.find(mst, e.gp1), this.find(mst, e.gp2));
      } 
    }
    for (Edge e : inTree) {
      e.gp1.connect(e.gp2);
      e.gp1.addNeighbour(e.gp2);
      e.gp2.addNeighbour(e.gp1);
    }
    this.mst = inTree;
    this.radius = 1.0 / (this.longestDistance() / 2.0 + 1);
  }
  
  //finds the root note for the leaf first in the tree represented by source 
  GamePiece find(HashMap<GamePiece, GamePiece> source, GamePiece first) {
    GamePiece next = source.get(first);
    if (first.equals(source.get(next))) {
      return next;
    }
    return this.find(source, next);
  }
  
  //randomizes the connections in a board
  void randomizeBoard() {
    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece piece : row) {
        for (int i = this.rand.nextInt(3); i >= 0; i--) {
          piece.rotate();
        }
      }
    }
  }
  
  //computes the longest distance between the two tiles furthest from each other
  int longestDistance() {
    GamePiece powerStation = this.board.get(this.powerRow).get(this.powerCol);
    GamePiece furthest = powerStation;
    int longestDistance = 0;
    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece piece : row) {
        ArrayList<GamePiece> path = this.distanceBetween(piece, powerStation);
        if (longestDistance < path.size()) {
          furthest = path.get(0);
          longestDistance = path.size();
        }
      }
    }
    
    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece piece : row) {
        int dist = this.distanceBetween(piece, furthest).size();
        longestDistance = Math.max(dist, longestDistance);
      }
    }
    return longestDistance;
    
  }
 
  //returns the shortest path between two GamePieces  
  ArrayList<GamePiece> distanceBetween(GamePiece source, GamePiece target) {
    ArrayList<GamePiece> unvisited = new ArrayList<GamePiece>();
    HashMap<GamePiece, Integer> distances = new HashMap<GamePiece, Integer>();
    HashMap<GamePiece, GamePiece> predecessors = new HashMap<GamePiece, GamePiece>();
    
    unvisited.add(source);
    distances.put(source, 0);
    
    while (unvisited.size() > 0) {
      GamePiece v = unvisited.remove(0);
      for (GamePiece g: v.connected) {
        if (distances.get(g) == null || distances.get(g) > distances.get(v) + 1) {
          distances.put(g, distances.get(v) + 1);
          predecessors.put(g, v);
          unvisited.add(g);
        }
      }   
    }
    
    ArrayList<GamePiece> path = new ArrayList<GamePiece>();
    
    GamePiece step = target;
    
    if (predecessors.get(step) == null) {
      return path;
    }
    path.add(step);
    while (predecessors.get(step) != null) {
      step = predecessors.get(step);
      path.add(0,step);
    }
    return path;
  }

  // draws the board
  public WorldScene makeScene() {
    WorldScene base = this.getEmptyScene();
    return this.drawToScene(base);
  }

  // draws the board
  public WorldScene drawToScene(WorldScene scene) {

    int drawWidth = scene.width / this.width;
    int drawHeight = scene.height / this.height;
    for (int x = 0; x < this.width; x++) {
      for (int y = 0; y < this.height; y++) {
        GamePiece temp = this.board.get(x).get(y);
        WorldImage piece = this.board.get(x).get(y).drawPiece(drawWidth, drawHeight)
            .movePinhole(-drawWidth / 2, -drawHeight / 2);
        scene.placeImageXY(piece, temp.row * drawWidth, temp.col * drawHeight);
      }
    }
    return scene;
  }
  
  //checks if the game has been won
  public WorldEnd worldEnds() {
    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece piece : row) {
        if (piece.power <= 0) {
          return new WorldEnd(false, this.makeScene());
        }
      }
    }
    return new WorldEnd(true, this.makeWinScene());
  }

  //draws the scene when you win the game 
  WorldScene makeWinScene() {
    this.leaderboard.addScore(this.steps);
    try {
      this.leaderboard.saveToFile();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    
    
    WorldScene base = this.makeScene();
    TextImage text = new TextImage("You Win!", 60, Color.GREEN);
    long elapsed = Duration.between(this.startTime, Instant.now()).getSeconds();
    TextImage stats = new TextImage("Time Elapsed: " + elapsed + " seconds" + ", Steps Taken: " 
        + this.steps, 40, Color.WHITE);
    
    WorldImage overlay = new AboveImage(text, stats);
    int place = 1;
    for (int score : this.leaderboard.leaders()) {
      WorldImage scoreBoard = new TextImage(place + ": " + score, 40, Color.ORANGE);
      overlay = new AboveImage(overlay, scoreBoard);
      place += 1;
    }
    
    WorldImage background = new RectangleImage((int) overlay.getWidth()
        + 20, (int) overlay.getHeight() + 20,OutlineMode.SOLID,new Color(0.0f, 0.0f, 0.0f, 0.8f));
    WorldImage scene = new OverlayImage(overlay, background);
    
    base.placeImageXY(scene, base.width / 2, base.height / 2);
    
    return base;
  }

  // rotates the GamePiece at the mouseclick
  public void onMouseClicked(Posn p, String button) {
    GamePiece gp = this.pieceAtScenePos(p, this.getEmptyScene());
    gp.rotate();
    this.updateBoard();
    this.steps += 1;
  }

  // rotates a game piece based on the click of the mouse
  public void onKeyEvent(String key) {
    if (key.equals("r")) {
      this.generateBoard();
      this.randomizeBoard();
      this.updateBoard();
    }
    if (key.equals("g")) {
      this.generateBoard();
    }
    if (key.equals("left")) {
      if (this.powerRow - 1 >= 0 && this.board.get(this.powerRow - 1).get(this.powerCol)
          .isConnected(this.board.get(this.powerRow).get(this.powerCol))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.powerRow -= 1;
        this.board.get(this.powerRow).get(this.powerCol).powerStation = true;
        this.steps += 1;
      }
    }
    if (key.equals("right")) {
      if (this.powerRow + 1 < this.width && this.board.get(this.powerRow + 1).get(this.powerCol)
          .isConnected(this.board.get(this.powerRow).get(this.powerCol))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.powerRow += 1;
        this.board.get(this.powerRow).get(this.powerCol).powerStation = true;
        this.steps += 1;
      }
    }
    if (key.equals("up")) {
      if (this.powerCol - 1 >= 0 && this.board.get(this.powerRow).get(this.powerCol - 1)
          .isConnected(this.board.get(this.powerRow).get(this.powerCol))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.powerCol -= 1;
        this.board.get(this.powerRow).get(this.powerCol).powerStation = true;
        this.steps += 1;
      }
    }
    if (key.equals("down")) {
      if (this.powerCol + 1 < this.height && this.board.get(this.powerRow).get(this.powerCol + 1)
          .isConnected(this.board.get(this.powerRow).get(this.powerCol))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.powerCol += 1;
        this.board.get(this.powerRow).get(this.powerCol).powerStation = true;
        this.steps += 1;
      }
    }
    this.updateBoard();
  }

  // returns the cell at the point where the mouse was clicked
  GamePiece pieceAtScenePos(Posn p, WorldScene scene) {
    if (p.x > scene.width || p.y > scene.height) {
      throw new RuntimeException("Mouse out of bounds");
    }
    double xr = (double) p.x / scene.width;
    double yr = (double) p.y / scene.height;

    int x = (int) (xr * this.width);
    int y = (int) (yr * this.height);
    return this.board.get(x).get(y);
  }

  // updates the power on the board
  void updateBoard() {
    GamePiece powerStation = this.board.get(this.powerRow).get(this.powerCol);
    for (ArrayList<GamePiece> r : this.board) {
      for (GamePiece gp : r) {
        gp.power = 0;
      }
    }
    powerStation.power = 1;

    int row = this.powerRow;
    int col = this.powerCol;
    ArrayList<Posn> updated = new ArrayList<Posn>(
        Arrays.asList(new Posn(this.powerRow, this.powerCol)));
    for (int x = row - 1; x <= row + 1; x++) {
      for (int y = col - 1; y <= col + 1; y++) {
        if (x < this.width && y < this.height && y >= 0 && x >= 0
            && !updated.contains(new Posn(x, y))) {
          this.updateHelper(x, y, row, col, updated);
        }
      }
    }
  }

  // setting the power of a game piece and its neighbors
  void updateHelper(int row, int col, int fromRow, int fromCol, ArrayList<Posn> updated) {
    GamePiece source = this.board.get(fromRow).get(fromCol);
    GamePiece next = this.board.get(row).get(col);

    if (!source.isConnected(next) && !updated.contains(new Posn(row, col))) {
      return;
    }

    next.power = Math.max(source.power - this.radius, 0);
    updated.add(new Posn(row, col));
    for (int x = row - 1; x <= row + 1; x++) {
      for (int y = col - 1; y <= col + 1; y++) {
        if (x < this.width && y < this.height && y >= 0 && x >= 0
            && !updated.contains(new Posn(x, y))) {
          this.updateHelper(x, y, row, col, updated);
        }
      }
    }
  }
}

//represents the top scores of the game
class Leaderboard {
  ArrayList<Integer> scores;

  Leaderboard() {
    this.scores = new ArrayList<Integer>();
    BufferedReader file = null;
    try {
      file = new BufferedReader(new FileReader("scores.csv"));
      String line = file.readLine();
      while (line != null) {
        int score = Integer.parseInt(line);
        this.scores.add(score);
        line = file.readLine();
      }
      
    }
    catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (file != null) {
        try {
          file.close();
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  //locally saves the scores to a file
  void saveToFile() throws IOException {
    FileOutputStream file = new FileOutputStream("scores.csv");
    for (int score : this.scores) {
      String line = score + "\n";
      file.write(line.getBytes());
    }
    file.close();
  }

  //add a score to the Leaderboard
  void addScore(int score) {
    this.scores.add(score);
  }

  //returns up to the top 5 scores on the Leaderboard 
  List<Integer> leaders() {
    Collections.sort(this.scores, new AscendingIntSort());
    return this.scores.subList(0, Math.min(this.scores.size(), 5));
  }
}

//sorts numbers in ascending order 
class AscendingIntSort implements Comparator<Integer> {

  public int compare(Integer o1, Integer o2) {
    return o1 - o2;
  }
}

class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  double power;
  ArrayList<GamePiece> connected;

  GamePiece(int row, int col, boolean powerStation, double power) {
    this.row = row;
    this.col = col;
    this.left = false;
    this.right = false;
    this.top = false;
    this.bottom = false;
    this.powerStation = powerStation;
    this.power = power;
    this.connected = new ArrayList<GamePiece>();
  }
  
  //adds the given GamePiece as a neighbor to this 
  void addNeighbour(GamePiece g) {
    this.connected.add(g);
  }
  
  //connects two GamePieces,
  //throws an exception if theyre not touching
  public void connect(GamePiece other) {
    if (this.row == other.row) {
      if (this.col == other.col - 1) {
        this.bottom = true; 
        other.top = true;
      }
      else if (this.col == other.col + 1) {
        this.top = true;
        other.bottom = true;
      }
    }
    else if (this.col == other.col) {
      if (this.row == other.row - 1) {
        this.right = true; 
        other.left = true;
      }
      else if (this.row == other.row + 1) {
        this.left = true; 
        other.right = true;
      }
    }
    else {
      throw new RuntimeException("GamePieces are not connected");
    }
  }

  // draws the GamePiece
  public WorldImage drawPiece(int width, int height) {
    WorldImage rt = new RectangleImage(width, height, OutlineMode.SOLID, Color.DARK_GRAY);
    Color wireColor = this.power > 0.0
        ? new Color(195 + (int) (this.power * 60), 195 + (int) (this.power * 60), 155)
        : Color.PINK;
    if (this.left) {
      rt = new OverlayImage(new RectangleImage(width / 2, height / 6, OutlineMode.SOLID,wireColor)
          .movePinhole(width / 4, 0), rt);
    }
    if (this.right) {
      rt = new OverlayImage(new RectangleImage(width / 2, height / 6, OutlineMode.SOLID,wireColor)
          .movePinhole(-width / 4, 0), rt);
    }
    if (this.top) {
      rt = new OverlayImage(new RectangleImage(width / 6, height / 2, OutlineMode.SOLID,wireColor)
          .movePinhole(0, height / 4), rt);
    }
    if (this.bottom) {
      rt = new OverlayImage(new RectangleImage(width / 6, height / 2, OutlineMode.SOLID,wireColor)
          .movePinhole(0, -height / 4), rt);
    }
    if (this.powerStation) {
      rt = new OverlayImage(
          new StarImage(Math.min(width, height) / 2, 7, OutlineMode.SOLID, Color.CYAN), rt);
    }
    rt = new OverlayImage(new RectangleImage(width, height, OutlineMode.OUTLINE,Color.BLACK), rt);
    return rt;
  }

  // rotates the GamePiece
  void rotate() {
    boolean oldRight = this.right;
    this.right = this.top;
    this.top = this.left;
    this.left = this.bottom;
    this.bottom = oldRight;
  }

  // checks if a game piece is connected to its neighbors
  boolean isConnected(GamePiece other) {
    if (this.row == other.row) {
      if (this.col == other.col - 1) {
        return this.bottom && other.top;
      }
      else if (this.col == other.col + 1) {
        return this.top && other.bottom;
      }
    }
    else if (this.col == other.col) {
      if (this.row == other.row - 1) {
        return this.right && other.left;
      }
      else if (this.row == other.row + 1) {
        return this.left && other.right;
      }
    }
    return false;
  }
  
  //checks if this GamePiece is the same as other
  public boolean equals(Object other) {
    if (!(other instanceof GamePiece)) {
      return false;
    }
    GamePiece gp = (GamePiece) other;
    return this.row == gp.row && this.col == gp.col 
        && this.left == gp.left && this.right == gp.right 
        && this.top == gp.top && this.bottom == gp.bottom 
        && this.powerStation == gp.powerStation && this.power == gp.power;
  }
  
  //returns a HashCode for this
  public int hashCode() {
    return this.row * 4091 + this.col * 8839;
  }
}

//sorts edges by their weight in ascending order 
class AscendingWeightSort implements Comparator<Edge> {

  public int compare(Edge o1, Edge o2) {
    return o1.weight - o2.weight;
  }
}

//represents an edge between two adjacent GamePieces
class Edge {
  GamePiece gp1;
  GamePiece gp2;
  int weight;
  
  Edge(GamePiece gp1, GamePiece gp2, int weight) {
    this.gp1 = gp1;
    this.gp2 = gp2;
    this.weight = weight;
  }
}


class ExamplesLight {

  GamePiece gp1;
  GamePiece gp2;
  GamePiece gp3;
  GamePiece gp5;

  LightEmAll l1;
  
  Leaderboard ldb1;

  void initData() {
    this.gp3 = new GamePiece(0, 1, false, 0.2);
    this.gp2 = new GamePiece(0, 2, false, 0);
    this.gp1 = new GamePiece(0, 3, false, 0.4);
    this.gp5 = new GamePiece(0, 3, true, 0);
    this.gp3.left = true;
    this.gp3.bottom = true;
    this.gp2.top = true;

    this.l1 = new LightEmAll(3, 3, new Random(3));
    
    
    try {
      PrintWriter file = new PrintWriter("scores.csv");
      file.print("");
      file.close();
    }
    catch (FileNotFoundException e1) {
      e1.printStackTrace();
    }
    
    this.ldb1 = new Leaderboard();
  }

  void testDrawPiece(Tester t) {
    this.initData();
    int width = 5;
    int height = 3;
    WorldImage rec = new RectangleImage(width, height, OutlineMode.SOLID, Color.DARK_GRAY);

    WorldImage rt = new OverlayImage(
        new RectangleImage(width, height, OutlineMode.OUTLINE, Color.BLACK),
        new RectangleImage(width, height, OutlineMode.SOLID, Color.DARK_GRAY));

    t.checkExpect(this.gp1.drawPiece(5, 3), rt);

    WorldImage ps = new OverlayImage(
        new RectangleImage(width, height, OutlineMode.OUTLINE, Color.BLACK), new OverlayImage(
            new StarImage(Math.min(width, height) / 2, 7, OutlineMode.SOLID, Color.CYAN), rec));
    t.checkExpect(this.gp5.drawPiece(5, 3), ps);

    GamePiece gp4 = new GamePiece(0, 3, false, 0);
    t.checkExpect(gp4.drawPiece(5, 3), rt);

    t.checkExpect(this.gp3.drawPiece(5, 3),
        new OverlayImage(new RectangleImage(5, 3, OutlineMode.OUTLINE, Color.BLACK),
            new OverlayImage(new RectangleImage(0,1, OutlineMode.SOLID, new Color(207, 207, 155)),
                new OverlayImage(
                    new RectangleImage(2, 0, OutlineMode.SOLID, new Color(207, 207, 155))
                        .movePinhole(1, 0),
                    new RectangleImage(5, 3, OutlineMode.SOLID, Color.DARK_GRAY)))));
  }

  void testDrawToScene(Tester t) {
    this.initData();

    WorldScene scene = new WorldScene(100, 100);
    int drawWidth = scene.width / this.l1.width;
    int drawHeight = scene.height / this.l1.height;

    WorldImage piece = this.l1.board.get(0).get(0).drawPiece(drawWidth, drawHeight);

    scene.placeImageXY(piece.movePinhole(-drawWidth / 2, -drawHeight / 2), 0 * drawWidth,
        0 * drawHeight);

    t.checkExpect(this.l1.board.get(0).get(0).drawPiece(drawWidth, drawHeight), piece);

    WorldImage piece2 = this.l1.board.get(0).get(1).drawPiece(drawWidth, drawHeight);
    scene.placeImageXY(piece2.movePinhole(-drawWidth / 2, -drawHeight / 2), 0 * drawWidth,
        1 * drawHeight);

    t.checkExpect(this.l1.board.get(0).get(1).drawPiece(drawWidth, drawHeight), piece2);

    WorldImage piece3 = this.l1.board.get(0).get(2).drawPiece(drawWidth, drawHeight);
    scene.placeImageXY(piece3.movePinhole(-drawWidth / 2, -drawHeight / 2), 0 * drawWidth,
        2 * drawHeight);

    t.checkExpect(this.l1.board.get(0).get(2).drawPiece(drawWidth, drawHeight), piece3);

    WorldImage piece4 = this.l1.board.get(1).get(0).drawPiece(drawWidth, drawHeight);
    scene.placeImageXY(piece4.movePinhole(-drawWidth / 2, -drawHeight / 2), 1 * drawWidth,
        0 * drawHeight);

    t.checkExpect(this.l1.board.get(1).get(0).drawPiece(drawWidth, drawHeight), piece4);

    WorldImage piece5 = this.l1.board.get(1).get(1).drawPiece(drawWidth, drawHeight);
    scene.placeImageXY(piece5.movePinhole(-drawWidth / 2, -drawHeight / 2), 1 * drawWidth,
        1 * drawHeight);

    t.checkExpect(this.l1.board.get(1).get(1).drawPiece(drawWidth, drawHeight), piece5);

    WorldImage piece6 = this.l1.board.get(1).get(2).drawPiece(drawWidth, drawHeight);
    scene.placeImageXY(piece6.movePinhole(-drawWidth / 2, -drawHeight / 2), 1 * drawWidth,
        2 * drawHeight);

    t.checkExpect(this.l1.board.get(1).get(2).drawPiece(drawWidth, drawHeight), piece6);

    WorldImage piece7 = this.l1.board.get(2).get(0).drawPiece(drawWidth, drawHeight);
    scene.placeImageXY(piece7.movePinhole(-drawWidth / 2, -drawHeight / 2), 2 * drawWidth,
        0 * drawHeight);

    t.checkExpect(this.l1.board.get(2).get(0).drawPiece(drawWidth, drawHeight), piece7);

    WorldImage piece8 = this.l1.board.get(2).get(1).drawPiece(drawWidth, drawHeight);
    scene.placeImageXY(piece8.movePinhole(-drawWidth / 2, -drawHeight / 2), 2 * drawWidth,
        1 * drawHeight);

    t.checkExpect(this.l1.board.get(2).get(1).drawPiece(drawWidth, drawHeight), piece8);

    WorldImage piece9 = this.l1.board.get(2).get(2).drawPiece(drawWidth, drawHeight);
    scene.placeImageXY(piece9.movePinhole(-drawWidth / 2, -drawHeight / 2), 2 * drawWidth,
        2 * drawHeight);

    t.checkExpect(this.l1.board.get(2).get(2).drawPiece(drawWidth, drawHeight), piece9);

    t.checkExpect(this.l1.drawToScene(new WorldScene(100, 100)), scene);
  }

  // cant test this as it uses this.getEmptyScene() which uses width and height
  // from bigBang window
  // this function just calls on DrawToScene() which was tesged above
  void testMakeScene(Tester t) {
    this.initData();
  }

  void testPieceAtScenePos(Tester t) {
    this.initData();
    WorldScene scene = new WorldScene(50, 50);
    Posn p = new Posn(60, 50);
    Posn p2 = new Posn(6, 9);
    Posn p3 = new Posn(45, 49);
    GamePiece gp1 = new GamePiece(0, 0, true, 0);
    GamePiece gp2 = new GamePiece(2, 2, false, 0);
    gp1.bottom = false;
    gp2.left = false;
    gp2.right = true;
    gp2.top = false;
    gp2.bottom = true;
    gp1.top = true;
    gp1.powerStation = true;
    gp1.power = 1.0;
    
    t.checkException(new RuntimeException("Mouse out of bounds"), this.l1, "pieceAtScenePos", p,
        scene);
    t.checkExpect(this.l1.pieceAtScenePos(p2, scene).equals(gp1), true);
    t.checkExpect(this.l1.pieceAtScenePos(p3, scene).equals(gp2), true);
  }

  void testRotate(Tester t) {
    this.initData();
    t.checkExpect(this.gp1.left, false);
    t.checkExpect(this.gp1.right, false);
    t.checkExpect(this.gp1.top, false);
    t.checkExpect(this.gp1.bottom, false);

    t.checkExpect(this.gp3.left, true);
    t.checkExpect(this.gp3.right, false);
    t.checkExpect(this.gp3.top, false);
    t.checkExpect(this.gp3.bottom, true);

    this.gp1.rotate();
    this.gp3.rotate();

    t.checkExpect(this.gp1.left, false);
    t.checkExpect(this.gp1.right, false);
    t.checkExpect(this.gp1.top, false);
    t.checkExpect(this.gp1.bottom, false);

    t.checkExpect(this.gp3.left, true);
    t.checkExpect(this.gp3.right, false);
    t.checkExpect(this.gp3.top, true);
    t.checkExpect(this.gp3.bottom, false);
  }

  void testOnMouseClicked(Tester t) {
    this.initData();
    Posn n = new Posn(4, 4);
    WorldScene scene = new WorldScene(100, 100);
    GamePiece g1 = this.l1.pieceAtScenePos(n, scene);

    t.checkExpect(g1.left, false);
    t.checkExpect(g1.right, false);
    t.checkExpect(g1.top, true);
    t.checkExpect(g1.bottom, false);

    g1.rotate();
    t.checkExpect(g1.left, false);
    t.checkExpect(g1.right, true);
    t.checkExpect(g1.top, false);
    t.checkExpect(g1.bottom, false);
  }

  void testIsConnected(Tester t) {
    this.initData();
    t.checkExpect(this.gp3.isConnected(this.gp2), true);
    t.checkExpect(this.gp2.isConnected(this.gp3), true);
    t.checkExpect(this.gp2.isConnected(this.gp1), false);
    t.checkExpect(this.gp1.isConnected(this.gp2), false);
  }

  void testGenerateBoard(Tester t) {
    this.initData();

    this.l1.generateBoard();
    t.checkExpect(this.l1.board.get(0).get(0).bottom, false);
    t.checkExpect(this.l1.board.get(0).get(1).bottom, true);
    t.checkExpect(this.l1.board.get(0).get(2).bottom, false);
    t.checkExpect(this.l1.board.get(0).get(2).powerStation, false);
    t.checkExpect(this.l1.board.get(1).get(0).bottom, true);
    t.checkExpect(this.l1.board.get(1).get(0).right, true);
    t.checkExpect(this.l1.board.get(1).get(0).left, true);
    t.checkExpect(this.l1.board.get(1).get(1).left, true);
    t.checkExpect(this.l1.board.get(1).get(1).top, true);
    t.checkExpect(this.l1.board.get(1).get(1).bottom, false);
    t.checkExpect(this.l1.board.get(1).get(2).top, false);
    t.checkExpect(this.l1.board.get(1).get(2).bottom, false);
    t.checkExpect(this.l1.board.get(2).get(0).top, false);
    t.checkExpect(this.l1.board.get(2).get(0).right, false);
    t.checkExpect(this.l1.board.get(2).get(0).left, true);
    t.checkExpect(this.l1.board.get(2).get(1).left, true);
    t.checkExpect(this.l1.board.get(2).get(1).right, false);
    t.checkExpect(this.l1.board.get(2).get(2).right, false);
    t.checkExpect(this.l1.board.get(2).get(2).top, true);
    t.checkExpect(this.l1.board.get(2).get(2).left, false);
  }
  
  //make win scene
  
  void testAscendingWeightSort(Tester t) {
    this.initData();
    
    AscendingWeightSort aws = new AscendingWeightSort();
    Edge e1 = new Edge(this.gp1, this.gp2, 1);
    Edge e2 = new Edge(this.gp2, this.gp1, 1);
    Edge e3 = new Edge(this.gp3, this.gp5, 2);
    
    t.checkExpect(aws.compare(e1, e2), 0);
    t.checkExpect(aws.compare(e2, e1), 0);
    t.checkExpect(aws.compare(e2, e3), -1);
    t.checkExpect(aws.compare(e3, e2), 1);
  }
  
  void testHashCode(Tester t) {
    this.initData();
    t.checkExpect(this.gp1.hashCode(), 26517);
    t.checkExpect(this.gp2.hashCode(), 17678);
    this.gp1.top = true;
    t.checkExpect(this.gp1.hashCode(), 26517);
  }
  
  void testEquals(Tester t) {
    this.initData();
    GamePiece temp = new GamePiece(this.gp1.row, this.gp1.col, false, this.gp1.power);
    
    t.checkExpect(this.gp1.equals(temp), true);
    t.checkExpect(this.gp1.equals(this.gp2), false);
    t.checkExpect(this.gp2.equals(this.gp1), false);
  }
  
  void testAddNeighbour(Tester t) {
    this.initData();
    GamePiece temp = new GamePiece(0, 0, true, 1);
    t.checkExpect(temp.connected.contains(this.gp1), false);
    temp.addNeighbour(this.gp1);
    t.checkExpect(temp.connected.contains(this.gp1), true);
  }
  
  void testConnect(Tester t) {
    this.initData();
    GamePiece top = new GamePiece(0, 0, false, 0);
    GamePiece bot = new GamePiece(0, 1, false, 0);
    
    t.checkExpect(top.isConnected(bot), false);
    t.checkExpect(top.bottom, false);
    t.checkExpect(bot.top, false);
    top.connect(bot);
    t.checkExpect(top.isConnected(bot), true);
    t.checkExpect(top.bottom, true);
    t.checkExpect(bot.top, true);
    t.checkException(new RuntimeException("GamePieces are not connected"), top, "connect", 
        new GamePiece(4, 5, false, 0));
  }
  
  void testAscendingIntSort(Tester t) {
    this.initData();
    AscendingIntSort ais = new AscendingIntSort();
    int i1 = 1;
    int i2 = 1;
    int i3 = 2;
    
    t.checkExpect(ais.compare(i1, i2), 0);
    t.checkExpect(ais.compare(i2, i1), 0);
    t.checkExpect(ais.compare(i3, i1), 1);
  }
  
  void testAddScore(Tester t) {
    this.initData();
    
    t.checkExpect(this.ldb1.scores, new ArrayList<Integer>());
    this.ldb1.addScore(34);
    t.checkExpect(this.ldb1.scores, new ArrayList<Integer>(Arrays.asList(34)));
    this.ldb1.addScore(12);
    t.checkExpect(this.ldb1.scores, new ArrayList<Integer>(Arrays.asList(34, 12)));
    this.ldb1.addScore(13);
    t.checkExpect(this.ldb1.scores, new ArrayList<Integer>(Arrays.asList(34, 12, 13)));
  }
  
  void testSaveToFile(Tester t) {
    this.initData();
    Leaderboard lead = new Leaderboard();
    lead.addScore(10);
    lead.addScore(4);
    lead.addScore(2);
    lead.addScore(3);
    lead.leaders();
    try {
      lead.saveToFile();
    }
    catch (IOException e1) {
      e1.printStackTrace();
    }
    
    BufferedReader outFile = null;
    try {
      outFile = new BufferedReader(new FileReader("scores.csv"));
    }
    catch (FileNotFoundException e1) {
      e1.printStackTrace();
    }
    String line = null;
    String s = "";
    try {
      while ((line = outFile.readLine()) != null) {
        s += line + ",";
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    t.checkExpect(s, "2,3,4,10,");
    
  }
  
  //distanceBetween, randomize, makeWinScene
  
  void testLongestDistance(Tester t) {
    this.initData();
    LightEmAll l2 = new LightEmAll(20, 20, new Random(4));
    
    t.checkExpect(this.l1.longestDistance(), 7);
    t.checkExpect(l2.longestDistance(), 165);
  }
  
  void testDistanceBetween(Tester t) {
    this.initData();
    GamePiece g1 = this.l1.board.get(0).get(0);
    GamePiece g2 = this.l1.board.get(1).get(0);
    GamePiece g3 = this.l1.board.get(2).get(0);
    
    t.checkExpect(this.l1.distanceBetween(g1, g2), new ArrayList<GamePiece>(Arrays.asList(g1, g2)));
    t.checkExpect(this.l1.distanceBetween(g1, g3), 
        new ArrayList<GamePiece>(Arrays.asList(g1, g2, g3)));
  }
  
  void testRandomize(Tester t) {
    this.initData();
    t.checkExpect(this.l1.board.get(0).get(0).bottom, false);
    t.checkExpect(this.l1.board.get(0).get(1).bottom, false);
    t.checkExpect(this.l1.board.get(0).get(2).bottom, true);
    t.checkExpect(this.l1.board.get(0).get(2).powerStation, false);
    t.checkExpect(this.l1.board.get(1).get(0).bottom, false);
    t.checkExpect(this.l1.board.get(1).get(0).right, true);
    t.checkExpect(this.l1.board.get(1).get(0).left, true);
    t.checkExpect(this.l1.board.get(1).get(1).left, false);
    t.checkExpect(this.l1.board.get(1).get(1).top, true);
    t.checkExpect(this.l1.board.get(1).get(1).bottom, true);
    t.checkExpect(this.l1.board.get(1).get(2).top, true);
    t.checkExpect(this.l1.board.get(1).get(2).bottom, true);
    t.checkExpect(this.l1.board.get(2).get(0).top, true);
    t.checkExpect(this.l1.board.get(2).get(0).right, true);
    t.checkExpect(this.l1.board.get(2).get(0).left, false);
    t.checkExpect(this.l1.board.get(2).get(1).left, false);
    t.checkExpect(this.l1.board.get(2).get(1).right, true);
    t.checkExpect(this.l1.board.get(2).get(2).right, true);
    t.checkExpect(this.l1.board.get(2).get(2).top, false);
    t.checkExpect(this.l1.board.get(2).get(2).left, false);
    this.l1.randomizeBoard();
    t.checkExpect(this.l1.board.get(0).get(0).bottom, false);
    t.checkExpect(this.l1.board.get(0).get(1).bottom, false);
    t.checkExpect(this.l1.board.get(0).get(2).bottom, false);
    t.checkExpect(this.l1.board.get(0).get(2).powerStation, false);
    t.checkExpect(this.l1.board.get(1).get(0).bottom, false);
    t.checkExpect(this.l1.board.get(1).get(0).right, true);
    t.checkExpect(this.l1.board.get(1).get(0).left, true);
    t.checkExpect(this.l1.board.get(1).get(1).left, false);
    t.checkExpect(this.l1.board.get(1).get(1).top, true);
    t.checkExpect(this.l1.board.get(1).get(1).bottom, true);
    t.checkExpect(this.l1.board.get(1).get(2).top, false);
    t.checkExpect(this.l1.board.get(1).get(2).bottom, false);
    t.checkExpect(this.l1.board.get(2).get(0).top, true);
    t.checkExpect(this.l1.board.get(2).get(0).right, false);
    t.checkExpect(this.l1.board.get(2).get(0).left, true);
    t.checkExpect(this.l1.board.get(2).get(1).left, true);
    t.checkExpect(this.l1.board.get(2).get(1).right, true);
    t.checkExpect(this.l1.board.get(2).get(2).right, true);
    t.checkExpect(this.l1.board.get(2).get(2).top, true);
    t.checkExpect(this.l1.board.get(2).get(2).left, false);
  }
  
  void testMakeWinScene(Tester t) {
    this.initData();
    
    this.ldb1.addScore(34);
    try {
      this.ldb1.saveToFile();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    
    
    WorldScene base = this.l1.makeScene();
    TextImage text = new TextImage("You Win!", 60, Color.GREEN);
    long elapsed = Duration.between(this.l1.startTime, Instant.now()).getSeconds();
    TextImage stats = new TextImage("Time Elapsed: " + elapsed + " seconds" + ", Steps Taken: " 
        + this.l1.steps, 40, Color.WHITE);
    
    WorldImage overlay = new AboveImage(text, stats);
    int place = 1;
    for (int score : this.ldb1.leaders()) {
      WorldImage scoreBoard = new TextImage(place + ": " + score, 40, Color.ORANGE);
      overlay = new AboveImage(overlay, scoreBoard);
      place += 1;
    }
    
    WorldImage background = new RectangleImage((int) overlay.getWidth() + 20, 
        (int) overlay.getHeight() + 20, OutlineMode.SOLID, new Color(0.0f, 0.0f, 0.0f, 0.8f));
    WorldImage scene = new OverlayImage(overlay, background);
    
    base.placeImageXY(scene, base.width / 2, base.height / 2);
    
    t.checkExpect(this.l1.makeWinScene(), base);
  }
  
  void testFind(Tester t) {
    this.initData();
    HashMap<GamePiece, GamePiece> source = new HashMap<GamePiece, GamePiece>();
    source.put(this.gp1, this.gp1);
    source.put(this.gp2, this.gp1);
    source.put(this.gp3, this.gp2);
    
    t.checkExpect(this.l1.find(source, this.gp3), this.gp1);
  }
  
  void testWorldEnds(Tester t) {
    this.initData();
    
    t.checkExpect(this.l1.worldEnds(), new WorldEnd(false, this.l1.makeScene()));
    this.l1.onKeyEvent("g");
    t.checkExpect(this.l1.worldEnds(), new WorldEnd(false, this.l1.makeScene()));
    this.l1.onKeyEvent("right");
    t.checkExpect(this.l1.worldEnds(), new WorldEnd(false, this.l1.makeScene()));
    this.l1.onKeyEvent("down");
    t.checkExpect(this.l1.worldEnds(), new WorldEnd(true, this.l1.makeWinScene()));
    
  }
  
    void testBigBang(Tester t) {
      this.initData();
      World w1 = new LightEmAll(9, 9);
      w1.bigBang(1000, 1000, 1/24.0);
    }
}















