

class FluxSystem {
  boolean debug;

  int resolution, 
  cols, rows;
  ArrayList<Tile> cells;
  ArrayList<Tile> temp_cells;
  ArrayList<Integer> visitList;
 // ArrayList<TStatus> statusList;

  FluxSystem(int r) {

    resolution = r;
    cells = new ArrayList<Tile>();
    temp_cells = new ArrayList<Tile>();
    visitList = new ArrayList<Integer>();
    //statusList = new ArrayList<TStatus>();

    cols = width/resolution;
    rows = height/resolution;
    //init();
  }

  //setting up tiles
  void init() {

    int numCells = cols * rows;
    for (int id = 0; id < numCells; id++) {
      int _x = id%cols;
      int _y = id/cols;
      if (alpha(boundbox.get(_x*resolution, _y*resolution))>0) {

        cells.add(new Tile(id, _x*resolution, _y*resolution, resolution, this, cols, rows, false));
        temp_cells.add(new Tile(id, _x*resolution, _y*resolution, resolution, this, cols, rows, false));
      } else if (alpha(boundbox.get(_x*resolution, _y*resolution))<=0) {
        cells.add(new Tile(id, _x*resolution, _y*resolution, resolution, this, cols, rows, true));
        temp_cells.add(new Tile(id, _x*resolution, _y*resolution, resolution, this, cols, rows, true));
      }
    }

    println("TILE SIZE : "+ cells.size());
    println("TEMP_TILE SIZE : "+ temp_cells.size());
  }

  void update() {
    //add & update particles & tiles
    for (Tile c : cells) {
      c.update();
      if (cnt%300 ==0) {
        //println("count : "+cnt+" --//reset!!");
        c.cost=0;
      }
    }
  }

  void display() {
    for (Tile c : cells) {
      c.display();
    }

//    for (TStatus t : statusList) {
//      t.display();
//      if (t.onMouseOver(mouseX, mouseY)) {
//        break; // break the loop if a mouse over is detected, so it doesn't fire on overlapping tweets
//      }
//    }
  }

  //for Particle to look up it's location in the field
  PVector lookup(PVector lookup) {
    int column = int(constrain(lookup.x/resolution, 0, cols-1));
    int row = int(constrain(lookup.y/resolution, 0, rows-1));
    int id = row * cols + column;
    //Tile cell = cells.get(id);
    return cells.get(id).direction.get();
  }


  void addTweet(GeoLocation _tweetloc, String msg, String user) {
    //convert tweet location to screen position
    Location tweetloc = new Location(_tweetloc.getLatitude(), _tweetloc.getLongitude());
    ScreenPosition temp_sos  = map.getScreenPosition(tweetloc);
    PVector spos = new PVector(temp_sos.x, temp_sos.y);

    //find the cell based on input's position
    int column = int(constrain(spos.x/resolution, 0, cols-1));
    int row = int(constrain(spos.y/resolution, 0, rows-1));
    int id =  row * cols +column;
    float emo_val =  Sentiment(msg); //get emotion value

    //adding data to temporary cell
    Tile temp = temp_cells.get(id);
    temp.cost=emo_val;
    // println("id : "+ id + " added : "+ temp.cost + "  msg : "+ msg);

    temp.isGoal = true;
    if (temp.empty) {
      getField(id); //start calculating field

        //copy temp_cells' data to cells
      for (int i=0; i < cells.size (); i++) {
        Tile cell = cells.get(i);
        Tile t_cell = temp_cells.get(i);
        if (cell.cost==0 && !t_cell.isPassable) { // if cell's cost = 0 and temp_cell has cost
          cell.cost = t_cell.cost;
          cell.isPassable = t_cell.isPassable; 
          // cell.isGoal = t_cell.isGoal;//
        } else if (cell.cost!=0&&!cell.isPassable&&!t_cell.isPassable) { //if cell and temp_cell already have values 
          cell.cost=(cell.cost+t_cell.cost)*0.5; //normalize
        }
        t_cell.reset(); //reset tempcell for next inputs
      }
      for (Tile t : cells)calculateDirection(t);
    }

    statusList.add(new TStatus(msg, user, spos, emo_val));
  }   
  void testinput(PVector test, float ran) {

    PVector spos = new PVector(test.x, test.y);

    //find the cell based on input's position
    int column = int(constrain(spos.x/resolution, 0, cols-1));
    int row = int(constrain(spos.y/resolution, 0, rows-1));
    int id =  row * cols +column;

    //adding data to temporary cell
    Tile temp = temp_cells.get(id );
    temp.cost=ran;
    //println("test input - id : "+ id + " added : "+ temp.cost);

    temp.isGoal = true;
    if (temp.empty) {
      getField(id); //start calculating field

        //copy temp_cells' data to cells
      for (int i=0; i < cells.size (); i++) {
        Tile cell = cells.get(i);
        Tile t_cell = temp_cells.get(i);
        if (cell.cost==0 && !t_cell.isPassable) { // if cell's cost = 0 and temp_cell has cost
          cell.cost = t_cell.cost;
          cell.isPassable = t_cell.isPassable; 
          cell.isGoal = t_cell.isGoal;//
        } else if (cell.cost!=0&&!cell.isPassable&&!t_cell.isPassable) { //if cell and temp_cell already have values 
          cell.cost=(cell.cost+t_cell.cost)*0.5; //normalize
        }
        t_cell.reset(); //reset tempcell for next inputs
      }
      for (Tile t : cells)calculateDirection(t);
    }
  }   

  void getField(int id) {

    if (!visitList.contains(id))visitList.add(id); //if visitlist not contains id, add it
    while (visitList.size ()!=0) {

      int front = visitList.get(0); //get the first one, we are always evalutating the first one
      int targetX = front%cols; //get coulmn out of id
      int targetY = front/cols; //out row out of id
      Tile target = temp_cells.get(front);
      if (target.cost < 0) {
        findNeighbors(target, 1); //negative goes here
      } else if (target.cost > 0) {
        findNeighbors(target, -1); //pasitive
      }
      // println("tested : "+visitList.get(visitList.size ()-1));
      visitList.remove(0);
    }
    visitList.clear();//clear it every cycle;
  }

  void findNeighbors(Tile center, int invert) {

    center.isPassable = false;
    int step = 1 * invert;
    //TODO
    for (int i = 0; i < center.neighbors.length; i++) {

      if (center.neighbors[i] < 0 ) continue;      // check the boundary
      if (!temp_cells.get(center.neighbors[i]).isPassable)continue;
      Tile t = temp_cells.get(center.neighbors[i]);

      //TODO :: think about when center.cost is 0.5
      if (center.cost>0) { 
        float temp;
        //if (center.cost < 2)temp= constrain(center.cost - 0.1, 0, input_range);
        temp= constrain(center.cost + step, 0, input_range);
        t.cost = temp; //if the center one has positive val, we subtract -1
      }
      if (center.cost<0) {
        float temp;
        //if (center.cost > -2)temp= constrain(center.cost + 0.1, 0, input_range);
        temp= constrain(center.cost + step, -input_range, 0);
        t.cost = temp; //if the center one has positive val, we subtract -1
      }
      if (t.cost > 0 && t.isPassable) {
        t.isPassable=false;
        visitList.add(t.id);// add it self to the visitied list to find its neighbors
      } else if (t.isPassable && t.cost < 0) {
        t.isPassable=false;
        if (t.cost!=0)visitList.add(t.id);// add it self to the visitied list to find its neighbors
      }
    }
  }

  void calculateDirection(Tile center) {
    float X, Y;
    float N=0, E=0, S=0, W = 0;

    if (center.neighbors[0]>0)N = cells.get(center.neighbors[0]).cost;
    if (center.neighbors[1]>0)E = cells.get(center.neighbors[1]).cost;
    if (center.neighbors[2]>0)S = cells.get(center.neighbors[2]).cost;
    if (center.neighbors[3]>0)W = cells.get(center.neighbors[3]).cost;

    X = W-E;
    Y = N-S;
    //    X = E-W;
    //    Y = S-N;
    center.direction.set((-Y+X/2), (X+Y/2));
    //center.direction.set((-Y+X/2+X/2), (X+Y/2+Y/2));
    //    //check this article if interested
    //    //http://www.math.uic.edu/coursepages/math210/labpages/lab7
  }
}

