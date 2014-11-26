class FluxSystem {
  boolean debug;

  int resolution, 
  cols, rows;
  ArrayList<Tile> cells;
  ArrayList<Tile> temp_cells;
  ArrayList<Integer> visitList;

  FluxSystem(int r) {

    resolution = r;
    cells = new ArrayList<Tile>();
    temp_cells = new ArrayList<Tile>();
    visitList = new ArrayList<Integer>();

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
      cells.add(new Tile(id, _x*resolution, _y*resolution, resolution, this, cols, rows));
      temp_cells.add(new Tile(id, _x*resolution, _y*resolution, resolution, this, cols, rows));
    }

    println("TILE SIZE : "+ cells.size());
    println("TEMP_TILE SIZE : "+ temp_cells.size());
  }

  void update() {
    //add & update particles & tiles
    for (Tile c : cells) {
      c.update();
    }
  }

  void display() {
    for (Tile c : cells) {
      c.display();
    }
  }

  //for Particle to look up it's location in the field
  PVector lookup(PVector lookup) {
    int column = int(constrain(lookup.x/resolution, 0, cols-1));
    int row = int(constrain(lookup.y/resolution, 0, rows-1));
    int id = row * cols + column;
    //Tile cell = cells.get(id);
    return cells.get(id).direction.get();
  }


  void addTweet(GeoLocation _tweetloc, String msg) {
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
  void testinput(PVector test, float ran) {

    PVector spos = new PVector(test.x, test.y);

    //find the cell based on input's position
    int column = int(constrain(spos.x/resolution, 0, cols-1));
    int row = int(constrain(spos.y/resolution, 0, rows-1));
    int id =  row * cols +column;
    float emo_val =  ran; //get emotion value

    //adding data to temporary cell
    Tile temp = temp_cells.get(id );
    temp.cost=emo_val;
    //println("test input - id : "+ id + " added : "+ temp.cost);

    temp.isGoal = true;
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

   // for (Tile t : cells)calculateDirection(t);
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
      } else {
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
      t.cost = center.cost + step; //if the center one has positive val, we subtract -1

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

    for (int i : center.neighbors) {
      //N->E->S->W
      if (i!=-1) { //out of boundary indexs are -1, check Tile constructor
        N = temp_cells.get(center.neighbors[0]).cost;
        E = temp_cells.get(center.neighbors[1]).cost;
        S = temp_cells.get(center.neighbors[2]).cost;
        W = temp_cells.get(center.neighbors[3]).cost;
      }
      //if neighbors indexs are out of bound, the cost gonna be 0
    }
    X = W-E;
    Y = N-S;

    center.direction.set(Y-X/2, -X-Y/2);
    //check this article if interested
    //http://www.math.uic.edu/coursepages/math210/labpages/lab7
  }
}

