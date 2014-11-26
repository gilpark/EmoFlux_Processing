class FluxSystem {
  boolean debug;
  int X, //columns
  Y, //rows
  resolution, 
  cell_ID;
  // int column;
  // int row;
  ArrayList<Tile> cells;
  ArrayList<Tile> temp_cells;
  ArrayList<Integer> visitList;

  FluxSystem(int r) {

    resolution = r;
    cells = new ArrayList<Tile>();
    temp_cells = new ArrayList<Tile>();
    visitList = new ArrayList<Integer>();

    X = width/resolution;
    Y = height/resolution;
    //init();
  }

  //setting up tiles
  void init() {

    int numCells = X * Y;
    for (int id = 0; id < numCells; id++) {
      int _x = id%X;
      int _y = id/X;
      cells.add(new Tile(_x*resolution, _y*resolution, id, resolution, this));
      temp_cells.add(new Tile(_x*resolution, _y*resolution, id, resolution, this));
    }

    println("TILE SIZE : "+ cells.size());
    println("TEMP_TILE SIZE : "+ temp_cells.size());
  }

  void update() {
    //add & update particles
    for (int i = 0; i < cells.size (); i++) {
      Tile cell = cells.get(i);
      cell.update(); //includes particle display
    }
  }

  void display() {
    //println("flux display");
    for (int i = 0; i < cells.size (); i++) {
      Tile cell = cells.get(i);
      cell.display();
    }
  }

  //for Particle looing up it's location in the field
  PVector lookup(PVector lookup) {

    int column = int(constrain(lookup.x/resolution, 0, X-1));
    int row = int(constrain(lookup.y/resolution, 0, Y-1));
    int id = row * X + column;
    Tile cell = cells.get(id);
    return cell.direction.get();
  }


  void addTweet(GeoLocation _tweetloc, String msg) {

    //convert tweet location to screen position
    Location tweetloc = new Location(_tweetloc.getLatitude(), _tweetloc.getLongitude());
    ScreenPosition temp_sos  = map.getScreenPosition(tweetloc);
    PVector spos = new PVector(temp_sos.x, temp_sos.y);
    //println(spos);
    //ilist.add(new Test_input(pos, Sentiment(msg)));  

    //find the cell based on input
    int column = int(constrain(spos.x/resolution, 0, X-1));
    int row = int(constrain(spos.y/resolution, 0, Y-1));
    int id =  row * X +column;
    float emo_val = (int)Sentiment(msg); //get emotion value
  ///asdfkjal;sdkfjla;sdfjals;dfkjad
    //adding data to temporary cell
    Tile temp = temp_cells.get(id);
    temp.cost=emo_val;
    temp.isGoal = true;
    calculateField(id); //start calculating field
    //copy temp_cells' data to cells
    for (int i=cells.size ()-1; i>=0; i--) {
      Tile cell = cells.get(i);
      Tile t_cell = temp_cells.get(i);

      if (cell.cost==0 && !t_cell.isPassible) // if cell's cost = 0 and temp_cell has cost
       cell.cost = t_cell.cost;
      cell.isPassible = t_cell.isPassible;
      cell.isGoal = t_cell.isGoal;//
      cell.direction.add(t_cell.direction);
      //cell = t_cell;
      if (cell.cost!=0&&!cell.isPassible&&!t_cell.isPassible) { //if cell and temp_cell already have values 
        if (cell.cost*t_cell.cost<0)//positive&negative
        {
          cell.cost=cell.cost+t_cell.cost;
        }
        if (cell.cost*t_cell.cost>0)//positive&positive or negative&negative
        {
                  cell.cost =(cell.cost+ t_cell.cost)*.5; //normalize

//          if (t_cell.cost>0) {
//            if (cell.cost>t_cell.cost) {
//              cell.cost = cell.cost;
//            }
//            //cell.cost =(cell.cost+ t_cell.cost)*01.5; //normalize
//            if (cell.cost<t_cell.cost)
//              cell.cost = t_cell.cost;
//            //TODO: VETOR SUM??
//          }
//          if (t_cell.cost<0) {
//            if (cell.cost<t_cell.cost) {
//              cell.cost = cell.cost;
//            }
//            //cell.cost =(cell.cost+ t_cell.cost)*01.5; //normalize
//            if (cell.cost>t_cell.cost)
//              cell.cost = t_cell.cost;
//            //TODO: VETOR SUM??
//          }
        }
      }

      t_cell.reset(); //reset tempcell
      //calculateVecs(i);// then calculate vectors in the cells
    }

    for (int i=0; i<cells.size (); i++)calculateVecs(i);
    // then calculate vectors in the cells
  }   
  
  void calculateVecs(int _id) {
    int _x = _id%X;
    int _y = _id/X;
    int _column = int(constrain(_x, 0, X-1));
    int _row = int(constrain(_y, 0, Y-1));

    int N, E, S, W;
    N = (_row-1) * X + _column;
    S = (_row+1) * X + _column;
    E = _row * X + _column+1;
    W = _row * X + _column-1;

    Tile center = cells.get(_id);

    float vecX, vecY;
    vecX = 0;
    vecY = 0;
    if (_x<=0) {
      Tile right = cells.get(E);
      vecX = right.cost - 0;
    }
    if (_x>=X-1) {
      Tile left = cells.get(W);
      vecX = 0 - left.cost;
    }
    if (_y <= 0) {
      Tile down = cells.get(S);
      vecY = down.cost - 0;
    }
    if (_y >= Y-1) {
      Tile up = cells.get(N);
      vecY = 0 - up.cost;
    }
    if (_x > 0 && _x < X-1) {
      Tile right = cells.get(E);
      Tile left = cells.get(W);
      vecX = center.direction.x + (right.cost - left.cost);
    }
    if (_y > 0 && _y < Y-1) {
      Tile up = cells.get(N);
      Tile down = cells.get(S);
      vecY = center.direction.y + (down.cost - up.cost);
    }

    center.direction.set(vecX, vecY);
  }


  void calculateField(int id) {
    if (!visitList.contains(id))visitList.add(id); //if visitlist not contains id, add it

    while (visitList.size ()!=0) {
      int front = visitList.get(0); //get the first one, we are always evalutating the first one
      int targetX = front%X; //get coulmn out of id
      int targetY = front/X; //out row out of id
      Tile target = temp_cells.get(front);

      if (target.isPassible) {
        target.isPassible = false;
        if (targetY<=0||targetX<=0||targetY>=Y-1||targetX>=X-1)continue;
        if (target.cost != 0) { //interates it if the cell's cost is not 0
          findNeighbors(targetX, targetY);
        }
      }
      // println("tested : "+visitList.get(visitList.size ()-1));
      visitList.remove(0);
    }
    visitList.clear();//clear it every cycle;
  }
  //this is very ineffitiant way to find neighbors
  void findNeighbors(int _x, int _y) {
    int _column = int(constrain(_x, 0, X-1)); //constrain x cuz while fining neighbors, x&y can be smaller than 0
    int _row = int(constrain(_y, 0, Y-1)); //constrain y
    int id = _row * X + _column;
    int N, E, S, W;
    N = (_row-1) * X + _column;
    E = _row * X + _column+1;
    S = (_row+1) * X + _column;
    W = _row * X + _column-1;

    Tile up = temp_cells.get(N);
    Tile right = temp_cells.get(E);
    Tile down = temp_cells.get(S);
    Tile left = temp_cells.get(W);
    Tile center = temp_cells.get(id);


    if (up.isPassible) {
      if (center.cost>0) { //when the input val is positive
        up.cost = center.cost-1;
        up.direction.set(-1.5, 0);
      }
      if (center.cost<0) { //when the input val is negative
        up.cost = center.cost+1;
        up.direction.set(1.5, 0);
      }
      //println(id +"'s cost : "+center.cost+ " up : "+N+" up cost : "+ up.cost);

      visitList.add(N);
    }

    if (right.isPassible) {
      if (center.cost>0) { //when the input val is positive
        right.cost = center.cost-1;
        right.direction.set(0, -1.5);
      }
      if (center.cost<0) { //when the input val is negative
        right.cost = center.cost+1;
        right.direction.set(0, 1.5);
      }
      visitList.add(E);
    }

    if (down.isPassible) {
      if (center.cost>0) { //when the input val is positive
        down.cost = center.cost-1;
        down.direction.set(1.5, 0);
      }
      if (center.cost<0) { //when the input val is negative
        down.cost = center.cost+1;
        down.direction.set(-1.5, 0);
      }
      visitList.add(S);
    }


    if (left.isPassible) {
      if (center.cost>0) { //when the input val is positive
        left.cost = center.cost-1;
        left.direction.set(0, 1.5);
      }
      if (center.cost<0) { //when the input val is negative
        left.cost = center.cost+1;
        left.direction.set(0, -1.5);
      }
      visitList.add(W);
    }
  }
}

