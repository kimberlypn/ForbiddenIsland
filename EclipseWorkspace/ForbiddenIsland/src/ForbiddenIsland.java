// Assignment 9
// Nguyen, Kimberly
// kpnguyen
// Nguyen, Thien
// tnguyen11235

import java.util.*;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

/**
 * Comments: We implemented the scuba bell. It is hidden on the island somewhere,
 * and allows the player to swim for 10 seconds. There is also an option to make
 * the scuba visible, as commented in the drawScuba method. We also implemented
 * the keeping score whistle.
 */

// represents a single square of the game area
class Cell {
    // represents absolute height of this cell, in feet
    double height;
    // in logical coordinates, with the origin at the top-left corner of the
    // screen
    int x;
    int y;
    // the four adjacent cells to this one
    Cell left;
    Cell top;
    Cell right;
    Cell bottom;
    // reports whether this cell is flooded or not
    boolean isFlooded;
    // reports whether this cell is an ocean cell
    boolean isOcean;

    Cell(double height, int x, int y, Cell left, Cell top, Cell right, Cell bottom, 
            boolean isFlooded, boolean isOcean) {
        this.height = height;
        this.x = x;
        this.y = y;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.isFlooded = isFlooded;
        this.isOcean = false;
    }

    // convenience constructor that ignores links
    Cell(double height, int x, int y) {
        this.height = height;
        this.x = x;
        this.y = y;
    }

    // convenience constructor that only cares about this cell's coordinates
    Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // draws this cell
    WorldImage drawCell(int waterHeight) {
        int cellSize = ForbiddenIslandWorld.ISLAND_SIZE / 5;
        Color cellColor = new Color(0, 0, 0);
        // renders cells that are flooded from blue (meaning just below the water)
        // to black (meaning quite submerged)
        if (this.isFlooded && (top.isFlooded || left.isFlooded || right.isFlooded 
                || bottom.isFlooded)) {
            int r = (int) Math.abs(16 + (this.height - waterHeight) * .5);
            int g = 0;
            int b = (int) Math.abs(132 + (this.height - waterHeight) * 4);
            cellColor = new Color(r, g, b);
        }
        // renders cells that are below the current water level but are not flooded
        // from green (meaning just below the water level) to red (meaning dangerously
        // prone to flooding)
        else if (!(this.isFlooded) && (top.isFlooded || left.isFlooded 
                || right.isFlooded || bottom.isFlooded)) {
            int r = Math.abs((int) (((int) (this.height - waterHeight) * 215) / 32));
            int g = Math.abs((int) ((32 - Math.abs(this.height - waterHeight)) * 110) / 32);
            int b = 0;
            cellColor = new Color(r, g, b);
        }
        // renders cells that are above water from green (meaning just barely above water)
        // to white (meaning quite high)
        else {
            int r = Math.abs((int) ((int) -2 + Math.abs(this.height - waterHeight) * 8));
            int g = Math.abs((int) ((int) 100 + Math.abs(this.height - waterHeight) * 4));
            int b = Math.abs((int) ((int) -1 + Math.abs(this.height - waterHeight) * 8));
            cellColor = new Color(r, g, b);
        }
        return new RectangleImage(cellSize, cellSize, OutlineMode.SOLID, cellColor);
    }

    // draws this cell at its coordinates onto the given background
    WorldScene drawScene(WorldScene bg, int waterHeight) {
        bg.placeImageXY(this.drawCell(waterHeight), 
                this.x * ForbiddenIslandWorld.ISLAND_SCALE,
                this.y * ForbiddenIslandWorld.ISLAND_SCALE);
        return bg;
    }
}

// to represent an ocean cell
class OceanCell extends Cell {

    OceanCell(double height, int x, int y, Cell left, Cell top, Cell right, 
            Cell bottom, boolean isFlooded, boolean isOcean) {
        super(height, x, y, left, top, right, bottom, isFlooded, isOcean);
        this.isFlooded = true;
        this.isOcean = true;
    }

    // convenience constructor that ignores links
    OceanCell(double height, int x, int y) {
        super(height, x, y);
        this.isFlooded = true;
        this.isOcean = true;
    }

    // draws this cell
    WorldImage drawCell(int waterHeight) {
        int cellSize = ForbiddenIslandWorld.ISLAND_SIZE / 5;
        RectangleImage flooded = new RectangleImage(cellSize, cellSize, 
                OutlineMode.SOLID, new Color(19, 0, 134));
        return flooded;
    }

    // draws this cell at its coordinates onto the given background
    WorldScene drawScene(WorldScene bg, int waterHeight) {
        bg.placeImageXY(this.drawCell(waterHeight), 
                this.x * ForbiddenIslandWorld.ISLAND_SCALE,
                this.y * ForbiddenIslandWorld.ISLAND_SCALE);
        return bg;
    }
}

// to represent a pilot
class Pilot {
    // corresponds to the cell that the pilot is on
    Cell cell;
    // counts the number of steps this pilot has taken
    int steps;
    // true if the pilot has the swimming suit on and can swim through flooded cells
    boolean canSwim;

    Pilot(Cell cell, int steps, boolean canSwim) {
        this.cell = cell;
        this.steps = steps;
        this.canSwim = false;
    }

    // produces the image of this pilot
    WorldImage pilotImage() {
        return new FromFileImage("pilot-icon.png");
    }
}

// to represent everything that the player needs to pick up
class Target {
    // corresponds to the cell that the target is on
    Cell cell;
    // true if the target has been collected
    boolean isCollected;

    Target(Cell cell, boolean isCollected) {
        this.cell = cell;
        this.isCollected = isCollected;
    }

    // produces the image of this target
    WorldImage targetImage() {
        return new FromFileImage("target.png");
    }

    // removes the image of this target if the pilot has "collected" it
    WorldImage drawTarget() {
        if (this.isCollected) {
            return new CircleImage(0, OutlineMode.OUTLINE, Color.WHITE);
        } 
        else {
            return this.targetImage();
        }
    }

    // draws this cell at its coordinates onto the given background
    WorldScene drawScene(WorldScene bg) {
        bg.placeImageXY(this.drawTarget(), this.cell.x * ForbiddenIslandWorld.ISLAND_SCALE,
                this.cell.y * ForbiddenIslandWorld.ISLAND_SCALE);
        return bg;
    }
}

// to represent the final helicopter target that can only be "picked up" after all
// other targets have been picked up
class HelicopterTarget extends Target {
    // true if the pilot has successfully collected the helicopter
    boolean pilotSafe;

    HelicopterTarget(Cell cell, boolean isCollected, boolean pilotSafe) {
        super(cell, isCollected);
        this.cell.x = ForbiddenIslandWorld.ISLAND_HEIGHT;
        this.cell.y = ForbiddenIslandWorld.ISLAND_HEIGHT;
        this.isCollected = isCollected; // true if all other targets have been collected
        this.pilotSafe = pilotSafe;
    }

    // produces the image of this helicopter
    WorldImage helicopterImage() {
        return new FromFileImage("helicopter.png");
    }

    // removes the image of this helicopter target once the pilot can and has
    // "collected" it
    WorldImage drawHelicopter() {
        if (this.isCollected) {
            return new CircleImage(0, OutlineMode.OUTLINE, Color.WHITE);
        } 
        else {
            return this.helicopterImage();
        }
    }
}

// to represent an invisible, underwater swimming suit that allows the pilot to
// swim through flooded cells for a limited window of time
class Scuba {
    Cell cell;
    // to represent how many ticks the pilot has left to swim
    int time;

    Scuba(Cell cell, int time) {
        this.cell = cell;
        this.time = time;
    }

    // produces the image of this scuba
    WorldImage drawScuba() {
        return new CircleImage(0, OutlineMode.SOLID, Color.RED);
        /** Use the second return statement if you do not want the scuba to
         * be hidden.
         */
//        return new FromFileImage("scuba.png");
    }
}

// to represent a forbidden island world
class ForbiddenIslandWorld extends World {
    static final int ISLAND_SIZE = 64;
    static final int ISLAND_HEIGHT = 32;
    static final int ISLAND_SPEED = 10;
    static final int ISLAND_SCALE = 10;
    static int COUNT = 0;
    // all the cells of the game, including the ocean
    IList<Cell> board = new Empty<Cell>();
    // the current height of the ocean
    int waterHeight;
    // all the targets of the game
    IList<Target> targets = new Empty<Target>();
    HelicopterTarget helicopter;
    Pilot pilot;
    // counts how many targets are currently left in the game
    int targetsLeft;
    Scuba scuba;

    ForbiddenIslandWorld() {
        this.pilot = new Pilot(new Cell(200, 150), 0, false);
        this.scuba = new Scuba(new Cell(200, 150), 0);
        this.helicopter = 
                new HelicopterTarget(new Cell(ISLAND_HEIGHT, ISLAND_HEIGHT), 
                        false, false);
        this.initMountain();
    }

    // creates a 2D array list of the heights of the cells of this island
    public ArrayList<ArrayList<Double>> regularHeights() {
        ArrayList<ArrayList<Double>> boardHeight = new ArrayList<ArrayList<Double>>();
        for (int row = 0; row < ISLAND_SIZE + 1; row++) {
            ArrayList<Double> rows = new ArrayList<Double>();
            for (int col = 0; col < ISLAND_SIZE + 1; col++) {
                Double center = ISLAND_HEIGHT / 1.0;
                Double deltaX = Math.abs(center - row);
                Double deltaY = Math.abs(center - col);
                Double manhattanDist = deltaX + deltaY;
                rows.add(ISLAND_HEIGHT - manhattanDist);
            }
            boardHeight.add(rows);
        }
        return boardHeight;
    }

    // initializes heights to contain ISLAND_SIZE + 1 rows of ISLAND_SIZE + 1
    // columns of zeros, where the center of the grid is set to the island's height
    // and the midpoints of each side are set to 1.0
    public ArrayList<ArrayList<Double>> initZeroes() {
        ArrayList<ArrayList<Double>> boardGrid = new ArrayList<ArrayList<Double>>();
        for (int row = 0; row < ISLAND_SIZE + 1; row++) {
            ArrayList<Double> rows = new ArrayList<Double>();
            for (int col = 0; col < ISLAND_SIZE + 1; col++) {
                if (row == ISLAND_SIZE / 2 && col == ISLAND_SIZE / 2) {
                    rows.add(ISLAND_HEIGHT / 1.0);
                }
                if ((row == 0 && col == ISLAND_SIZE / 2) // top-mid
                        || (row == ISLAND_SIZE / 2 && col == 0) // left-mid
                        || (row == ISLAND_SIZE / 2 && col == ISLAND_SIZE) // right-mid
                        || (row == ISLAND_SIZE && col == ISLAND_SIZE / 2)) { // bottom-mid
                    rows.add(1.0);
                }
                else {
                    rows.add(0.0);
                }
            }
            boardGrid.add(rows);
        }
        return boardGrid;
    }
    
    // EFFECT: divides grid into quarters at every step, randomizing the heights of
    // the given 2D arraylist
    public void subDiv(Posn tl, Posn br, ArrayList<ArrayList<Double>> heights) {
        Posn bl = new Posn(tl.x, tl.y + 1);
        Posn tr = new Posn(br.x, br.y - 1);
        Posn t = new Posn(((tl.x + tr.x) / 2), tl.y);
        Posn b = new Posn(((bl.x + br.x) / 2), br.y);
        Posn l = new Posn(tl.x, ((bl.y + tl.y) / 2));
        Posn r = new Posn(br.x, ((tr.y + br.y) / 2));
        Posn m = new Posn(((tl.x + tr.x + bl.x + br.x) / 4), 
                ((tl.y + tr.y + bl.y + br.y) / 4));
        
        Double blHeight = heights.get(tl.x).get(br.y);
        Double trHeight = heights.get(br.x).get(tl.y);
        Double tlHeight = heights.get(tl.x).get(tl.y);
        Double brHeight = heights.get(br.x).get(br.y);
        
        // top-mid
        heights.get((tl.x + br.x) / 2).set(tl.y, (new Random().nextDouble() - 0.534 
                * (br.x - tl.x) + ((tlHeight + trHeight) / 2)));
        // bottom-mid
        heights.get((tl.x + br.x) / 2).set(br.y, (new Random().nextDouble() - 0.534 
                * (br.x - tl.x) + ((blHeight + brHeight) / 2)));
        // left-mid
        heights.get(tl.x).set((tl.y + br.y) / 2, (new Random().nextDouble() - 0.534 
                * (br.x - tl.x) + ((tlHeight + blHeight) / 2)));
        // right-mid
        heights.get(br.x).set((tl.y + br.y) / 2, (new Random().nextDouble() - 0.534 
                * (br.x - tl.x) + ((trHeight + brHeight) / 2)));
        // mid-mid
        heights.get((tl.x + br.x) / 2).set((tl.y + br.y) / 2, 
                (new Random().nextDouble() * (br.x - tl.x)
                + ((tlHeight + trHeight + blHeight + brHeight) / 4)));
        
        if ((br.x - tl.x) > 2) {
            subDiv(tl, m, heights); // quadrant II
            subDiv(t, r, heights); // quadrant I
            subDiv(l, b, heights); // quadrant III
            subDiv(m, br, heights); // quadrant IV
        }
    }
    
    // EFFECT: randomizes the heights of each double in the given 2D arraylist 
    // using the subdivision algorithm
    public void terrainHeights(ArrayList<ArrayList<Double>> heights) {
        Posn tl = new Posn(0, 0);
        Posn br = new Posn(ISLAND_SIZE, ISLAND_SIZE);
        Posn t = new Posn(ISLAND_SIZE / 2, 0);
        Posn l = new Posn(0, ISLAND_SIZE / 2);
        Posn r = new Posn(ISLAND_SIZE, ISLAND_SIZE / 2);
        Posn b = new Posn(ISLAND_SIZE / 2, ISLAND_SIZE);
        Posn m = new Posn(ISLAND_SIZE / 2, ISLAND_SIZE / 2);
        
        subDiv(tl, m, heights);
        subDiv(t, r, heights);
        subDiv(l, b, heights);
        subDiv(m, br, heights);
    }
    
    // creates a 2D array list of cells where each cell's height is determined
    // by the corresponding item in regularHeights
    public ArrayList<ArrayList<Cell>> arrCells(ArrayList<ArrayList<Double>> heights) {
        ArrayList<ArrayList<Cell>> boardCell = new ArrayList<ArrayList<Cell>>();
        for (int col = 0; col < ISLAND_SIZE + 1; col++) {
            ArrayList<Cell> cols = new ArrayList<Cell>();
            for (int row = 0; row < ISLAND_SIZE + 1; row++) {
                Double height = heights.get(col).get(row);
                if (height < this.waterHeight) {
                    cols.add(new OceanCell(height, row, col));
                } 
                else {
                    cols.add(new Cell(height, row, col));
                }
            }
            boardCell.add(cols);
        }
        return boardCell;
    }

    // creates a 2D array list of targets
    public ArrayList<ArrayList<Target>> arrTargets(ArrayList<ArrayList<Double>> heights) {
        ArrayList<ArrayList<Target>> boardTarget = new ArrayList<ArrayList<Target>>();
        for (int row = 0; row < 1; row++) {
            ArrayList<Target> rows = new ArrayList<Target>();
            // change the stopping condition to change the number of targets
            for (int col = 0; col < 5; col++) {
                rows.add(new Target(new Cell(row, col), false));
            }
            boardTarget.add(rows);
        }
        return boardTarget;
    }

    // produces an array of non-flooded cells
    public ArrayList<Cell> nonFloodedCell(ArrayList<ArrayList<Cell>> cells) {
        ArrayList<Cell> nonFlooded = new ArrayList<Cell>();
        for (int row = 0; row < cells.size(); row++) {
            for (int col = 0; col < cells.size(); col++) {
                if (!cells.get(row).get(col).isFlooded) {
                    nonFlooded.add(cells.get(row).get(col));
                }
            }
        }
        return nonFlooded;
    }

    // EFFECT: updates the pilot's cell and scuba's cell to a random non-flooded cell
    public void randomPilot(ArrayList<Cell> nonFlooded) {
        int rand1 = new Random().nextInt(nonFlooded.size());
        pilot.cell.x = nonFlooded.get(rand1).x * ISLAND_SCALE;
        pilot.cell.y = nonFlooded.get(rand1).y * ISLAND_SCALE;
        int rand2 = new Random().nextInt(nonFlooded.size());
        scuba.cell.x = nonFlooded.get(rand2).x * ISLAND_SCALE;
        scuba.cell.y = nonFlooded.get(rand2).y * ISLAND_SCALE;
    }

    // EFFECT: randomizes the locations of the targets of this island
    public void randomTargets(ArrayList<ArrayList<Target>> targets, 
            ArrayList<Cell> nonFlooded) {
        for (int row = 0; row < targets.size(); row++) {
            for (int col = 0; col < targets.get(row).size(); col++) {
                Target cur = targets.get(row).get(col);
                int rand = new Random().nextInt(nonFlooded.size());
                cur.cell.x = nonFlooded.get(rand).x;
                cur.cell.y = nonFlooded.get(rand).y;
            }
        }
    }

    // EFFECT: randomizes the heights of the cells of this island
    public void randomHeights(ArrayList<ArrayList<Cell>> cells) {
        for (int row = 0; row < cells.size(); row++) {
            for (int col = 0; col < cells.get(row).size(); col++) {
                Cell cur = cells.get(row).get(col);
                if (cur.height >= 0.0) {
                    cur.height = new Random().nextInt(32) + 1;
                }
            }
        }
    }

    // EFFECT: fixes up the top/bottom/left/right neighbor links of all cells
    // in this island
    public void fixLinks(ArrayList<ArrayList<Cell>> cells) {
        for (int row = 0; row < cells.size(); row++) {
            for (int col = 0; col < cells.get(row).size(); col++) {
                Cell cur = cells.get(row).get(col);
                // fixes up the top and bottom links
                if (row == 0) {
                    cur.top = cur;
                } 
                else {
                    cur.top = cells.get(row - 1).get(col);
                }
                if (row == ISLAND_SIZE) {
                    cur.bottom = cur;
                } 
                else {
                    cur.bottom = cells.get(row + 1).get(col);
                }
                // fixes up the left and right links
                if (col == 0) {
                    cur.left = cur;
                } 
                else {
                    cur.left = cells.get(row).get(col - 1);
                }
                if (col == ISLAND_SIZE) {
                    cur.right = cur;
                } 
                else {
                    cur.right = cells.get(row).get(col + 1);
                }
            }
        }
    }

    // EFFECT: fixes up the top/bottom/left/right neighbor links of the pilot's cell
    // and the scuba's cell
    public void fixPilotAndScuba(ArrayList<ArrayList<Cell>> cells) {
        this.fixLinks(cells);
        this.fixFlooded(cells);
        Cell newPilot = cells.get(this.pilot.cell.x / ISLAND_SCALE)
                .get(this.pilot.cell.y / ISLAND_SCALE);
        Cell newScuba = cells.get(this.scuba.cell.x / ISLAND_SCALE)
                .get(this.scuba.cell.y / ISLAND_SCALE);
        this.pilot.cell = newPilot;
        this.scuba.cell = newScuba;
    }

    // EFFECT: changes the target's isCollected field to true
    public void fixTargetCollected() {
        IListIterator<Target> iterTarget = new IListIterator<Target>(this.targets);
        while (iterTarget.hasNext()) {
            Target current = iterTarget.next();
            if ((current.cell.x == this.pilot.cell.x) 
                    && (current.cell.y == this.pilot.cell.y)) {
                current.isCollected = true;
                if (this.targetsLeft > 0) {
                    this.targetsLeft = this.targetsLeft - 1;
                }
            }
        }
    }

    // EFFECT: changes the helicopter's isCollected field to true once all the targets
    // have been collected and the pilot has "collected" it
    public void fixHelicopterCollected() {
        if ((this.targetsLeft <= 0) && (this.helicopter.cell.x == this.pilot.cell.x)
                && (this.helicopter.cell.y == this.pilot.cell.y)) {
            this.helicopter.isCollected = true;
            this.helicopter.pilotSafe = true;
        }
    }

    // EFFECT: changes the pilot's canSwim field to true if the pilot has found it
    // or to false if the pilot's allotted swim time has run out
    public void fixScubaActivation() {
        if ((this.scuba.cell.x == this.pilot.cell.x) 
                && (this.scuba.cell.y == this.pilot.cell.y)) {
            this.pilot.canSwim = true;
            this.scuba.time = 10;
        }
        if (this.scuba.time == 0) {
            this.pilot.canSwim = false;
        }
    }

    // EFFECT: determines whether the cells in this island are flooded and modifies
    // their isFlooded field accordingly
    public void fixFlooded(ArrayList<ArrayList<Cell>> cells) {
        this.fixLinks(cells);
        for (int row = 0; row < cells.size(); row++) {
            for (int col = 0; col < cells.get(row).size(); col++) {
                Cell cur = cells.get(row).get(col);
                if ((cur.height < this.waterHeight) && (cur.top.isFlooded ||
                        cur.bottom.isFlooded || cur.left.isFlooded || cur.right.isFlooded)) {
                    cur.isFlooded = true;
                }
            }
        }
    }

    // EFFECT: determines whether the cells in this island are flooded and modifies
    // their isFlooded fields accordingly on each tick
    public void tickFlood() {
        IListIterator<Cell> iterCell = new IListIterator<Cell>(this.board);
        while (iterCell.hasNext()) {
            Cell cur = iterCell.next();
            if ((cur.height < this.waterHeight) && (cur.top.isFlooded ||
                    cur.bottom.isFlooded || cur.left.isFlooded || cur.right.isFlooded)) {
                cur.isFlooded = true;
            }
        }
    }

    // EFFECT: initializes the perfectly regular mountain
    void initMountain() {
        this.waterHeight = 0;
        this.targetsLeft = 5;
        COUNT = 0;
        this.pilot.steps = 0;
        this.scuba.time = 0;
        Utils util = new Utils();
        ArrayList<ArrayList<Cell>> cells = this.arrCells(this.regularHeights());
        this.fixLinks(cells);
        this.fixFlooded(cells);
        this.board = util.flatten2D(cells);
        ArrayList<ArrayList<Target>> targets = this.arrTargets(this.regularHeights());
        this.randomTargets(targets, this.nonFloodedCell(cells));
        this.targets = util.flatten2D(targets);
        this.randomPilot(nonFloodedCell(cells));
        this.fixPilotAndScuba(cells);
    }

    // EFFECT: initializes the random mountain
    void initRandomMountain() {
        this.waterHeight = 0;
        this.targetsLeft = 5;
        COUNT = 0;
        this.pilot.steps = 0;
        this.scuba.time = 0;
        Utils util = new Utils();
        ArrayList<ArrayList<Cell>> cells = this.arrCells(this.regularHeights());
        this.randomHeights(cells);
        this.fixLinks(cells);
        this.fixFlooded(cells);
        this.board = util.flatten2D(cells);
        ArrayList<ArrayList<Target>> targets = this.arrTargets(this.regularHeights());
        this.randomTargets(targets, this.nonFloodedCell(cells));
        this.targets = util.flatten2D(targets);
        this.randomPilot(nonFloodedCell(cells));
        this.fixPilotAndScuba(cells);
    }

    // EFFECT: initializes the terrain
    void initTerrain() {
        this.waterHeight = 0;
        this.targetsLeft = 5;
        COUNT = 0;
        this.pilot.steps = 0;
        this.scuba.time = 0;
        Utils util = new Utils();
        ArrayList<ArrayList<Double>> zeroes = this.initZeroes();
        this.terrainHeights(zeroes);
        ArrayList<ArrayList<Cell>> cells = this.arrCells(zeroes);
        this.fixLinks(cells);
        this.fixFlooded(cells);
        this.board = util.flatten2D(cells);
        ArrayList<ArrayList<Target>> targets = this.arrTargets(this.regularHeights());
        this.randomTargets(targets, this.nonFloodedCell(cells));
        this.targets = util.flatten2D(targets);
        this.randomPilot(nonFloodedCell(cells));
        this.fixPilotAndScuba(cells);
    }

    // EFFECT: moves the pilot according to the given key
    public void onKeyEvent(String ke) {
        this.fixTargetCollected();
        this.fixHelicopterCollected();
        // when the player does not have the swimming suit on
        if (!this.pilot.canSwim) {
            if (ke.equals("left") && !(this.pilot.cell.left.isFlooded)) {
                this.pilot.cell = this.pilot.cell.left;
                this.pilot.steps = this.pilot.steps + 1;
            }
            else if (ke.equals("right") && !(this.pilot.cell.right.isFlooded)) {
                this.pilot.cell = this.pilot.cell.right;
                this.pilot.steps = this.pilot.steps + 1;
            }
            else if (ke.equals("down") && !(this.pilot.cell.bottom.isFlooded)) {
                this.pilot.cell = this.pilot.cell.bottom;
                this.pilot.steps = this.pilot.steps + 1;
            }
            else if (ke.equals("up") && !(this.pilot.cell.top.isFlooded)) {
                this.pilot.cell = this.pilot.cell.top;
                this.pilot.steps = this.pilot.steps + 1;
            }
            else if (ke.equals("m")) {
                this.initMountain();
            }
            else if (ke.equals("r")) {
                this.initRandomMountain();
            }
            else if (ke.equals("t")) {
                this.initTerrain();
            }
        }
        // when the player has the swimming suit on
        else if (this.pilot.canSwim) {
            if (ke.equals("left")) {
                this.pilot.cell = this.pilot.cell.left;
                this.pilot.steps = this.pilot.steps + 1;
            }
            else if (ke.equals("right")) {
                this.pilot.cell = this.pilot.cell.right;
                this.pilot.steps = this.pilot.steps + 1;
            }
            else if (ke.equals("down")) {
                this.pilot.cell = this.pilot.cell.bottom;
                this.pilot.steps = this.pilot.steps + 1;
            }
            else if (ke.equals("up")) {
                this.pilot.cell = this.pilot.cell.top;
                this.pilot.steps = this.pilot.steps + 1;
            }
            else if (ke.equals("m")) {
                this.initMountain();
            }
            else if (ke.equals("r")) {
                this.initRandomMountain();
            }
            else if (ke.equals("t")) {
                this.initTerrain();
            }
        }
    }
    
    // draws the island
    public WorldScene makeScene() {
        IListIterator<Cell> iterCell = new IListIterator<Cell>(this.board);
        IListIterator<Target> iterTarget = new IListIterator<Target>(this.targets);
        WorldImage stepsCounter = new TextImage("Steps Taken: " + 
            Integer.toString(this.pilot.steps), 15, Color.WHITE);
        WorldImage targetsCounter = new TextImage("Targets Left: " +
            Integer.toString(this.targetsLeft), 15,
                Color.WHITE);
        WorldImage helicopter = this.helicopter.drawHelicopter();
        WorldScene bg = new WorldScene(ISLAND_SIZE * ISLAND_SCALE, 
                ISLAND_SIZE * ISLAND_SCALE);
        while (iterCell.hasNext()) {
            Cell current = iterCell.next();
            if (current.isFlooded) {
                bg = current.drawScene(bg, waterHeight);
            }
            bg = current.drawScene(bg, waterHeight);
        }
        while (iterTarget.hasNext()) {
            Target current = iterTarget.next();
            bg = current.drawScene(bg);
        }
        bg.placeImageXY(this.pilot.pilotImage(), this.pilot.cell.x * ISLAND_SCALE, 
                this.pilot.cell.y * ISLAND_SCALE);
        bg.placeImageXY(helicopter, ISLAND_HEIGHT * ISLAND_SCALE, 
                ISLAND_HEIGHT * ISLAND_SCALE);
        bg.placeImageXY(stepsCounter, ISLAND_SIZE, ISLAND_SIZE - 50);
        bg.placeImageXY(targetsCounter, ISLAND_SIZE, ISLAND_SIZE - 30);
        bg.placeImageXY(this.scuba.drawScuba(), this.scuba.cell.x * ISLAND_SCALE, 
                this.scuba.cell.y * ISLAND_SCALE);
        WorldScene temp = bg;
        WorldImage scuba = new AboveImage(new TextImage("SCUBA ACTIVATED", 20, FontStyle.BOLD, 
                Color.CYAN), new TextImage("Time Left to Swim: " + 
                    Integer.toString(this.scuba.time),
                        15, Color.CYAN));
        // places the scuba timer on the screen if the pilot has activated it
        if (this.pilot.canSwim) {
            temp.placeImageXY(scuba, (ISLAND_SIZE * ISLAND_SCALE) / 2, 40);
            return temp;
        }
        // otherwise, returns bg without the scuba timer
        else {
            return temp;
        }
    }

    // EFFECT: floods the island at every tick
    public void onTick() {
        this.fixScubaActivation();
        ArrayList<ArrayList<Cell>> cells = this.arrCells(this.regularHeights());
        this.fixLinks(cells);
        this.fixFlooded(cells);
        if (COUNT == 10) {
            this.waterHeight = this.waterHeight + 1;
            this.tickFlood();
            COUNT = 0;
        } 
        else {
            COUNT++;
        }
        if (this.pilot.canSwim && this.scuba.time > 0) {
            this.scuba.time = this.scuba.time - 1;
        }
    }

    // displays the final image once all the cells have flooded
    public WorldEnd worldEnds() {
        WorldImage lose = new OverlayImage(
                new AboveImage(new TextImage("GAME OVER", 40, Color.WHITE),
                        new TextImage("STEPS TAKEN: " + Integer.toString(this.pilot.steps), 
                                25, Color.WHITE)),
                new RectangleImage(ISLAND_SIZE * ISLAND_SCALE, ISLAND_SIZE * ISLAND_SCALE, 
                        OutlineMode.SOLID,
                        Color.BLACK));
        WorldImage win = new OverlayImage(
                new AboveImage(new TextImage("YOU WON!", 40, Color.WHITE),
                        new TextImage("STEPS TAKEN: " + Integer.toString(this.pilot.steps), 
                                25, Color.WHITE)),
                new RectangleImage(ISLAND_SIZE * ISLAND_SCALE, ISLAND_SIZE * ISLAND_SCALE, 
                        OutlineMode.SOLID,
                        Color.BLACK));
        WorldImage drown = new OverlayImage(
                new AboveImage(new TextImage("YOU DROWNED.", 40, Color.WHITE),
                        new TextImage("STEPS TAKEN: " + Integer.toString(this.pilot.steps), 
                                25, Color.WHITE)),
                new RectangleImage(ISLAND_SIZE * ISLAND_SCALE, 
                        ISLAND_SIZE * ISLAND_SCALE, 
                        OutlineMode.SOLID,
                        Color.BLACK));
        WorldScene bg = new WorldScene(ISLAND_SIZE * ISLAND_SCALE, 
                ISLAND_SIZE * ISLAND_SCALE);
        IFunc<IList<Cell>, Boolean> func = new AllFlooded();
        // displays the winning image if the pilot successfully reaches the helicopter
        if (this.helicopter.pilotSafe) {
            bg.placeImageXY(win, (ISLAND_SIZE * ISLAND_SCALE) / 2, 
                    (ISLAND_SIZE * ISLAND_SCALE) / 2);
            return new WorldEnd(true, bg);
        }
        // displays the death image if the pilot's cell floods
        else if (this.pilot.cell.isFlooded && !this.pilot.canSwim) {
            bg.placeImageXY(drown, (ISLAND_SIZE * ISLAND_SCALE) / 2, 
                    (ISLAND_SIZE * ISLAND_SCALE) / 2);
            return new WorldEnd(true, bg);
        }
        // displays game over if all the cells on the board have flooded
        else if (func.apply(this.board)) {
            bg.placeImageXY(lose, (ISLAND_SIZE * ISLAND_SCALE) / 2, 
                    (ISLAND_SIZE * ISLAND_SCALE) / 2);
            return new WorldEnd(true, bg);
        } 
        else {
            return new WorldEnd(false, this.makeScene());
        }
    }
}

// to represent utilities
class Utils {
    // converts the given array list into an IList
    <T> IList<T> flatten(ArrayList<T> alist) {
        IList<T> temp = new Empty<T>();
        for (int i = 0; i < alist.size(); i++) {
            temp = temp.add(alist.get(i));
        }
        return temp;
    }

    // converts the given 2D array list into an IList
    <T> IList<T> flatten2D(ArrayList<ArrayList<T>> alist) {
        Utils util = new Utils();
        IList<T> temp = new Empty<T>();
        for (int row = 0; row < alist.size(); row++) {
            temp = temp.append(util.flatten(alist.get(row)));
        }
        return temp;
    }
}

// to represent one-argument function-objects with signature [T -> U]
interface IFunc<T, U> {
    // applies the method to the given T
    U apply(T t);
}

// function that checks if all the cells in the given list are flooded
class AllFlooded implements IFunc<IList<Cell>, Boolean> {
    // returns true if all the cells in the given list are flooded
    public Boolean apply(IList<Cell> cells) {
        IFunc<Cons<Cell>, Boolean> func = new AllFloodedHelp();
        if (!cells.isCons()) {
            return true;
        } 
        else {
            return func.apply((Cons<Cell>) cells);
        }
    }
}

// function that checks if all the cells in the given non-empty list are flooded
class AllFloodedHelp implements IFunc<Cons<Cell>, Boolean> {
    // returns true if all the cells in the given list are flooded
    public Boolean apply(Cons<Cell> cells) {
        IFunc<IList<Cell>, Boolean> func = new AllFlooded();
        return cells.first.isFlooded && func.apply(cells.rest);
    }
}

// to represent an iterator for lists
class IListIterator<T> implements Iterator<T> {
    IList<T> items;

    IListIterator(IList<T> items) {
        this.items = items;
    }

    // does the list have at least one more item?
    public boolean hasNext() {
        return this.items.isCons();
    }

    // gets the next item in the list
    // EFFECT: advances the iterator to the subsequent value
    public T next() {
        if (!this.hasNext()) {
            throw new IllegalArgumentException();
        }
        Cons<T> itemsAsCons = this.items.asCons();
        T answer = itemsAsCons.first;
        this.items = itemsAsCons.rest;
        return answer;
    }

    // EFFECT: removes the item just returned by next()
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

// to represent a list of T
interface IList<T> extends Iterable<T> {
    // calculates the size of this list
    int size();

    // adds the given item to this list
    IList<T> add(T given);

    // appends the given list onto this list
    IList<T> append(IList<T> given);

    // casts this list as a cons
    Cons<T> asCons();

    // is this a cons?
    boolean isCons();

    // creates an iterator for this list
    Iterator<T> iterator();
}

// to represent an empty list of T
class Empty<T> implements IList<T> {
    // calculates the size of this list
    public int size() {
        return 0;
    }

    // adds the given item to this list
    public IList<T> add(T given) {
        return new Cons<T>(given, this);
    }

    // appends the given list onto this list
    public IList<T> append(IList<T> given) {
        return given;
    }

    // casts this list as a cons
    public Cons<T> asCons() {
        throw new ClassCastException();
    }

    // is this a cons?
    public boolean isCons() {
        return false;
    }

    // creates an iterator for this list
    public Iterator<T> iterator() {
        return new IListIterator<T>(this);
    }
}

// to represent a non-empty list of T
class Cons<T> implements IList<T> {
    T first;
    IList<T> rest;

    Cons(T first, IList<T> rest) {
        this.first = first;
        this.rest = rest;
    }

    // calculates the size of this list
    public int size() {
        return 1 + this.rest.size();
    }

    // adds the given item to this list
    public IList<T> add(T given) {
        return this.append(new Cons<T>(given, new Empty<T>()));
    }

    // appends the given list onto this list
    public IList<T> append(IList<T> given) {
        return new Cons<T>(this.first, this.rest.append(given));
    }

    // casts this list as a cons
    public Cons<T> asCons() {
        return this;
    }

    // is this a cons?
    public boolean isCons() {
        return true;
    }

    // creates an iterator for this list
    public Iterator<T> iterator() {
        return new IListIterator<T>(this);
    }
}

// to represent examples and tests
class Examples {
    ForbiddenIslandWorld world;

    // to start the game
    void testGame(Tester t) {
        this.world = new ForbiddenIslandWorld();
        world.bigBang((10 * 64), (10 * 64), 1);
    }

    Cell centerTop = new Cell(ForbiddenIslandWorld.ISLAND_HEIGHT - 1, 
            ForbiddenIslandWorld.ISLAND_SIZE / 2, 
            (ForbiddenIslandWorld.ISLAND_SIZE / 2) - 1);
    Cell centerBottom = new Cell(ForbiddenIslandWorld.ISLAND_HEIGHT - 1, 
            ForbiddenIslandWorld.ISLAND_SIZE / 2,
            (ForbiddenIslandWorld.ISLAND_SIZE / 2) + 1);
    Cell centerLeft = new Cell(ForbiddenIslandWorld.ISLAND_HEIGHT - 1, 
            (ForbiddenIslandWorld.ISLAND_SIZE / 2) - 1,
            ForbiddenIslandWorld.ISLAND_SIZE / 2);
    Cell centerRight = new Cell(ForbiddenIslandWorld.ISLAND_HEIGHT - 1, 
            (ForbiddenIslandWorld.ISLAND_SIZE / 2) + 1,
            ForbiddenIslandWorld.ISLAND_SIZE / 2);
    Cell center = new Cell(ForbiddenIslandWorld.ISLAND_HEIGHT, 
            ForbiddenIslandWorld.ISLAND_SIZE / 2,
            ForbiddenIslandWorld.ISLAND_SIZE / 2, this.centerLeft, this.centerTop, 
            this.centerRight, this.centerBottom, false, false);
    Cell topMid = new Cell(ForbiddenIslandWorld.ISLAND_HEIGHT / 2, 
            ForbiddenIslandWorld.ISLAND_SIZE / 2, 0);
    Cell leftMid = new Cell(ForbiddenIslandWorld.ISLAND_HEIGHT / 2, 0, 
            ForbiddenIslandWorld.ISLAND_SIZE / 2);
    Cell rightMid = new Cell(ForbiddenIslandWorld.ISLAND_HEIGHT / 2, 
            ForbiddenIslandWorld.ISLAND_SIZE,
            ForbiddenIslandWorld.ISLAND_SIZE / 2);
    Cell bottomMid = new Cell(ForbiddenIslandWorld.ISLAND_HEIGHT / 2, 
            ForbiddenIslandWorld.ISLAND_SIZE / 2,
            ForbiddenIslandWorld.ISLAND_SIZE);
    Cell c1 = new OceanCell(0, 0, 0);

    IList<Cell> mt = new Empty<Cell>();
    IList<Cell> loc1 = new Cons<Cell>(this.center, new Cons<Cell>(this.topMid,
            new Cons<Cell>(this.leftMid, new Cons<Cell>(this.rightMid, 
                    new Cons<Cell>(this.bottomMid, this.mt)))));
    IList<Cell> loc2 = new Cons<Cell>(this.center, new Cons<Cell>(this.c1, this.mt));
    IList<Cell> loc3 = new Cons<Cell>(this.c1, this.mt);
    Cons<Cell> loc4 = new Cons<Cell>(this.center, new Cons<Cell>(this.topMid,
            new Cons<Cell>(this.leftMid, new Cons<Cell>(this.rightMid, 
                    new Cons<Cell>(this.bottomMid, this.mt)))));
    Cons<Cell> loc5 = new Cons<Cell>(this.c1, this.mt);
    Cons<Cell> loc6 = new Cons<Cell>(this.center, this.mt);

    Utils util = new Utils();
    ArrayList<Cell> cells = new ArrayList<Cell>();
    ArrayList<ArrayList<Cell>> arrCells = new ArrayList<ArrayList<Cell>>();

    IListIterator<Cell> iterMt = new IListIterator<Cell>(this.mt);
    IListIterator<Cell> iterCons = new IListIterator<Cell>(this.loc1);
    IListIterator<Cell> iterCons2 = new IListIterator<Cell>(this.loc6);

    Pilot p1 = new Pilot(new Cell(20, 15), 0, false);
    Pilot p2 = new Pilot(new Cell(64, 64), 0, false);
    Pilot p3 = new Pilot(new Cell(0, 0), 0, false);

    Target t1 = new Target(new Cell(18, 26), false);
    Target t2 = new Target(new Cell(18, 26), true);

    HelicopterTarget h1 = new HelicopterTarget(
            new Cell(ForbiddenIslandWorld.ISLAND_HEIGHT, 
                    ForbiddenIslandWorld.ISLAND_HEIGHT), false, false);
    HelicopterTarget h2 = new HelicopterTarget(
            new Cell(ForbiddenIslandWorld.ISLAND_HEIGHT, 
                    ForbiddenIslandWorld.ISLAND_HEIGHT), true, true);

    IFunc<IList<Cell>, Boolean> allFlooded = new AllFlooded();
    IFunc<Cons<Cell>, Boolean> allFloodedHelp = new AllFloodedHelp();

    // to initialize the data for tests
    void initData() {
        this.world = new ForbiddenIslandWorld();
        this.cells = new ArrayList<Cell>();
        this.cells.add(this.center);
        this.cells.add(this.topMid);
        this.cells.add(this.leftMid);
        this.cells.add(this.rightMid);
        this.cells.add(this.bottomMid);
        this.arrCells = new ArrayList<ArrayList<Cell>>();
        this.arrCells.add(this.cells);
        this.arrCells.add(this.cells);
        this.iterMt = new IListIterator<Cell>(this.mt);
        this.iterCons = new IListIterator<Cell>(this.loc1);
    }

    // to test the method pilotImage
    void testPilotImage(Tester t) {
        t.checkExpect(p1.pilotImage(), new FromFileImage("pilot-icon.png"));
    }

    // to test the method drawScene in the Target class
    void testDrawTargetScene(Tester t) {
        WorldScene bg = new WorldScene(640, 640);
        bg.placeImageXY(t1.drawTarget(), 180, 260);
        t.checkExpect(t1.drawScene(new WorldScene(640, 640)), bg);
    }

    // to test the method targetImage
    void testTargetImage(Tester t) {
        t.checkExpect(t1.targetImage(), new FromFileImage("target.png"));
    }

    // to test the method drawTarget
    void testDrawTarget(Tester t) {
        t.checkExpect(t1.drawTarget(), t1.targetImage());
        t.checkExpect(t2.drawTarget(), new CircleImage(0, OutlineMode.OUTLINE, 
                Color.WHITE));
    }

    // to test the method helicopterImage
    void testHelicopterImage(Tester t) {
        t.checkExpect(h1.helicopterImage(), new FromFileImage("helicopter.png"));
    }

    // to test the method drawHelicopter
    void testDrawHelicopter(Tester t) {
        t.checkExpect(h1.drawHelicopter(), h1.helicopterImage());
        t.checkExpect(h2.drawHelicopter(), new CircleImage(0, OutlineMode.OUTLINE, 
                Color.WHITE));
    }

    // to test the method regularHeights
    void testRegularHeights(Tester t) {
        this.initData();
        t.checkExpect(world.regularHeights().get(0).get(0), -32.0);
        t.checkExpect(world.regularHeights().get(32).get(32), 32.0);
        t.checkExpect(world.regularHeights().get(0).get(16), -16.0);
        t.checkExpect(world.regularHeights().get(16).get(0), -16.0);
        t.checkExpect(world.regularHeights().get(32).get(16), 16.0);
        t.checkExpect(world.regularHeights().get(16).get(32), 16.0);
    }

    // to test the method initZeroes
    void testInitZeroes(Tester t) {
        this.initData();
        t.checkExpect(world.initZeroes().get(0).get(0), 0.0);
        t.checkExpect(world.initZeroes().get(32).get(32), 32.0);
        t.checkExpect(world.initZeroes().get(32).get(0), 1.0);
    }
   
    // to test the method subDiv
    void testSubDiv(Tester t) {
        this.initData();
        ArrayList<ArrayList<Double>> zeroes = world.initZeroes();
        world.subDiv(new Posn(32, 32), new Posn(64, 64), zeroes);
        t.checkNumRange(zeroes.get(32).get(32), -33, 33);
        t.checkNumRange(zeroes.get(0).get(0), 0, 1);
    }
    
    // to test the method terrainHeights
    void testTerrainHeights(Tester t) {
        this.initData();
        ArrayList<ArrayList<Double>> zeroes = world.initZeroes();
        world.subDiv(new Posn(32, 32), new Posn(64, 64), zeroes);
        world.terrainHeights(zeroes);
        t.checkNumRange(zeroes.get(0).get(0), 0, 1);
        t.checkNumRange(zeroes.get(25).get(15), -33, 33);
        t.checkNumRange(zeroes.get(32).get(32), -33, 33);
    }
    
    // to test the method arrCells
    void testArrCells(Tester t) {
        this.initData();
        t.checkExpect(world.arrCells(world.regularHeights()).get(0).get(0), 
                new OceanCell(-32.0, 0, 0));
        t.checkExpect(world.arrCells(world.regularHeights()).get(32).get(32), 
                new Cell(32.0, 32, 32));
        t.checkExpect(world.arrCells(world.regularHeights()).get(0).get(16), 
                new OceanCell(-16.0, 16, 0));
        t.checkExpect(world.arrCells(world.regularHeights()).get(16).get(0), 
                new OceanCell(-16.0, 0, 16));
        t.checkExpect(world.arrCells(world.regularHeights()).get(32).get(16), 
                new Cell(16.0, 16, 32));
        t.checkExpect(world.arrCells(world.regularHeights()).get(16).get(32), 
                new Cell(16.0, 32, 16));
    }

    // to test the method arrTargets
    void testArrTargets(Tester t) {
        this.initData();
        t.checkExpect(world.arrTargets(world.regularHeights()).get(0).get(1), 
                new Target(new Cell(0, 1), false));
        t.checkExpect(world.arrTargets(world.regularHeights()).get(0).get(2), 
                new Target(new Cell(0, 2), false));
        t.checkExpect(world.arrTargets(world.regularHeights()).get(0).get(3), 
                new Target(new Cell(0, 3), false));
    }

    // to test the method nonFloodedCell
    void testNonFloodedCell(Tester t) {
        this.initData();
        ArrayList<Cell> alist = new ArrayList<Cell>();
        alist.add(this.c1);
        alist.add(this.center);
        ArrayList<ArrayList<Cell>> doubleArr = new ArrayList<ArrayList<Cell>>();
        doubleArr.add(alist);
        t.checkExpect(world.nonFloodedCell(doubleArr), new ArrayList<Cell>());
    }

    // to test the method randomPilot
    void testRandomPilot(Tester t) {
        this.initData();
        ArrayList<ArrayList<Cell>> cells = world.arrCells(world.regularHeights());
        world.fixLinks(cells);
        world.fixFlooded(cells);
        world.randomPilot(world.nonFloodedCell(cells));
        t.checkRange(world.pilot.cell.x, 0, 640);
        t.checkRange(world.pilot.cell.y, 0, 640);
    }

    // to test the method randomHeights
    void testRandomHeights(Tester t) {
        this.initData();
        ArrayList<ArrayList<Cell>> cells = world.arrCells(world.regularHeights());
        world.fixLinks(cells);
        world.fixFlooded(cells);
        t.checkRange(cells.get(15).get(20).height, 
                ForbiddenIslandWorld.ISLAND_HEIGHT / -1.0,
                ForbiddenIslandWorld.ISLAND_HEIGHT / 1.0);
        t.checkRange(cells.get(64).get(64).height, 
                ForbiddenIslandWorld.ISLAND_HEIGHT / -1.0,
                ForbiddenIslandWorld.ISLAND_HEIGHT / 1.0);
    }

    // to test the method fixLinks
    void testFixLinks(Tester t) {
        this.initData();
        ArrayList<ArrayList<Cell>> cells = world.arrCells(world.regularHeights());
        world.fixLinks(cells);
        t.checkExpect(cells.get(32).get(32).left.x, 31);
        t.checkExpect(cells.get(32).get(32).left.y, 32);
        t.checkExpect(cells.get(32).get(32).right.x, 33);
        t.checkExpect(cells.get(32).get(32).right.y, 32);
        t.checkExpect(cells.get(32).get(32).top.x, 32);
        t.checkExpect(cells.get(32).get(32).top.y, 31);
        t.checkExpect(cells.get(32).get(32).bottom.x, 32);
        t.checkExpect(cells.get(32).get(32).bottom.y, 33);
    }

    // to test the method fixPilotAndScuba
    void testfixPilotAndScuba(Tester t) {
        this.initData();
        world.pilot = this.p1;
        t.checkExpect(world.pilot.cell.right, null);
        ArrayList<ArrayList<Cell>> cells = world.arrCells(world.regularHeights());
        world.fixPilotAndScuba(cells);
        t.checkExpect(world.pilot.cell.right.x * ForbiddenIslandWorld.ISLAND_SCALE, 20);
    }

    // to test the method fixTargetCollected
    void testFixTargetCollected(Tester t) {
        this.initData();
        world.targets = new Cons<Target>(t1, new Empty<Target>());
        t.checkExpect(t1.isCollected, false);
        t.checkExpect(world.targetsLeft, 5);
        world.pilot.cell.x = 18;
        world.pilot.cell.y = 26;
        world.fixTargetCollected();
        t.checkExpect(t1.isCollected, true);
        t.checkExpect(world.targetsLeft, 4);
    }

    // to test the method fixHelicopterCollected
    void testFixHelicopterCollected(Tester t) {
        this.initData();
        t.checkExpect(world.helicopter.isCollected, false);
        t.checkExpect(world.helicopter.pilotSafe, false);
        world.targetsLeft = 0;
        world.helicopter.cell.x = 32;
        world.helicopter.cell.y = 32;
        world.pilot.cell.x = 32;
        world.pilot.cell.y = 32;
        world.fixHelicopterCollected();
        t.checkExpect(world.helicopter.isCollected, true);
        t.checkExpect(world.helicopter.pilotSafe, true);
    }

    // to test the method fixFlooded
    void testFixFlooded(Tester t) {
        this.initData();
        ArrayList<ArrayList<Cell>> cells = world.arrCells(world.regularHeights());
        t.checkExpect(cells.get(0).get(0).isFlooded, true);
        t.checkExpect(cells.get(32).get(32).isFlooded, false);
        world.fixFlooded(cells);
        t.checkExpect(cells.get(0).get(0).isFlooded, true);
        t.checkExpect(cells.get(32).get(32).isFlooded, false);
    }

    // to test the method initMountain
    void testInitMountain(Tester t) {
        this.initData();
        t.checkExpect(world.waterHeight, 0);
        t.checkExpect(world.pilot.steps, 0);
    }

    // to test the method initRandomMountin
    void testInitRandomMountain(Tester t) {
        this.initData();
        t.checkExpect(world.waterHeight, 0);
        t.checkExpect(world.pilot.steps, 0);
    }

    // to test the method initTerrain
    void testInitTerrain(Tester t) {
        this.initData();
        t.checkExpect(world.waterHeight, 0);
        t.checkExpect(world.pilot.steps, 0);
    }

    // to test the method onKeyEvent
    void testOnKeyEvent(Tester t) {
        this.initData();
        world.onKeyEvent("left");
        t.checkExpect(world.pilot.steps, 1);
        world.onKeyEvent("up");
        t.checkExpect(world.pilot.steps, 2);
    }

    // to test the method onTick
    void testOnTick(Tester t) {
        this.initData();
        world.onTick();
        t.checkExpect(world.waterHeight, 0);
        world.onTick();
        t.checkExpect(world.waterHeight, 0);
    }

    // to test the method flatten
    void testFlatten(Tester t) {
        this.initData();
        t.checkExpect(this.util.flatten(this.cells), this.loc1);
        t.checkExpect(this.util.flatten(new ArrayList<Cell>()), this.mt);
    }

    // to test the method flatten2D
    void testFlatten2D(Tester t) {
        this.initData();
        t.checkExpect(this.util.flatten2D(this.arrCells), 
                this.loc1.append(this.loc1));
    }

    // to test the method apply method in the AllFlooded class
    void testAllFlooded(Tester t) {
        t.checkExpect(allFlooded.apply(this.mt), true);
        t.checkExpect(allFlooded.apply(this.loc1), false);
        t.checkExpect(allFlooded.apply(this.loc3), true);
    }

    // to test the method apply in the AllFlooded class
    void testAllFloodedHelp(Tester t) {
        t.checkExpect(allFloodedHelp.apply(this.loc4), false);
        t.checkExpect(allFloodedHelp.apply(this.loc5), true);
    }

    // to test the methods hasNext and next in the IListIterator class
    void testIListIterator(Tester t) {
        this.initData();
        t.checkExpect(this.iterMt.hasNext(), false);
        t.checkException(new IllegalArgumentException(), this.iterMt, "next");
        t.checkExpect(this.iterCons.hasNext(), true);
        t.checkExpect(this.iterCons.next(), this.center);
        t.checkExpect(this.iterCons.hasNext(), true);
        t.checkExpect(this.iterCons.next(), this.topMid);
        t.checkExpect(this.iterCons.hasNext(), true);
        t.checkExpect(this.iterCons.next(), this.leftMid);
        t.checkExpect(this.iterCons.hasNext(), true);
        t.checkExpect(this.iterCons.next(), this.rightMid);
        t.checkExpect(this.iterCons.hasNext(), true);
        t.checkExpect(this.iterCons.next(), this.bottomMid);
        t.checkExpect(this.iterCons.hasNext(), false);
        t.checkException(new IllegalArgumentException(), this.iterCons, "next");
    }

    // to test the method remove
    void testRemove(Tester t) {
        this.initData();
        t.checkException(new UnsupportedOperationException(), this.iterMt, "remove");
        t.checkException(new UnsupportedOperationException(), this.iterCons, "remove");
    }

    // to test the method size
    boolean testSize(Tester t) {
        return t.checkExpect(this.mt.size(), 0) && t.checkExpect(this.loc1.size(), 5);
    }

    // to test the method add
    boolean testAdd(Tester t) {
        return t.checkExpect(this.mt.add(this.center), new Cons<Cell>(this.center, this.mt))
                && t.checkExpect(new Cons<Cell>(this.center, this.mt).add(this.topMid),
                        new Cons<Cell>(this.center, new Cons<Cell>(this.topMid, this.mt)));
    }

    // to test the method append
    boolean testAppend(Tester t) {
        return t.checkExpect(this.mt.append(this.mt), this.mt) && 
               t.checkExpect(this.mt.append(this.loc1), this.loc1) && 
               t.checkExpect(this.loc1.append(this.mt), this.loc1) && 
               t.checkExpect(this.loc1.append(new Cons<Cell>(this.center, this.mt)),
                       new Cons<Cell>(this.center,
                                new Cons<Cell>(this.topMid, 
                                        new Cons<Cell>(this.leftMid, 
                                                new Cons<Cell>(this.rightMid,
                                                        new Cons<Cell>(this.bottomMid, 
                                                                new Cons<Cell>(this.center, 
                                                                        this.mt)))))));
    }

    // to test the method asCons
    boolean testAsCons(Tester t) {
        return t.checkException(new ClassCastException(), this.mt, "asCons")
                && t.checkExpect(this.loc1.asCons(), this.loc1);
    }

    // to test the method isCons
    boolean testIsCons(Tester t) {
        return t.checkExpect(this.mt.isCons(), false) && 
               t.checkExpect(this.loc1.isCons(), true);
    }

    // to test the method iterator
    boolean testIterator(Tester t) {
        return t.checkExpect(this.mt.iterator(), new IListIterator<Cell>(this.mt)) && 
                t.checkExpect(this.loc1.iterator(), new IListIterator<Cell>(this.loc1));
    }
}