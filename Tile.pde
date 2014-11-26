class Tile {
  int id, 
  size, //size of cell, defined by resolution
  cols, 
  rows, 
  particle_size;
  int[] neighbors;

  float cost, 
  cost2color, 
  x, y;

  boolean isGoal, 
  isPassable, // to check this cell can receive new value
  Dbugmode;

  PVector direction; //a vector indicates the direction of flow
  FluxSystem f;

  ArrayList<Particle> particles;
  color tile_color;

  Tile(int _id, float _x, float _y, int resolution, FluxSystem _f, int _cols, int _rows) {

    id = _id;
    x = _x;
    y = _y;
    f = _f;
    cols = _cols;
    rows = _rows;

    size = resolution;

    isGoal = false;
    isPassable = true;
    Dbugmode = debug;

    cost = 0;

    direction = new PVector(0, 0);
    particles = new ArrayList<Particle>();

    //find add neighbors in the array
    neighbors = new int[4];
    int column = id%cols; 
    int row = id/cols;
    //0 = N, 1 = E, 2 = S, 3 = W -> clockwise
    //THINK : is it better to make get neighbor function?

    if (column <= 0 || column >= cols-1) {
      neighbors[1] = -1; //if right one is out of boundary
      neighbors[3] = -1; //left
    } else if (row <= 0 && row >= row-1) {
      neighbors[0] = -1; //if upper one is out of boundary
      neighbors[2] = -1; //down
    } else { // if they aer in the boundary
      neighbors[0] = (row-1) * cols + column;
      neighbors[1] = row * cols + column+1;
      neighbors[2] = (row+1) * cols + column;
      neighbors[3] = row * cols + column-1;
    }
  }

  void reset() {
    cost = 0;
    isGoal = false;
    isPassable = true;
  }

  void update() {
    cost2color = map(cost, -input_range, input_range, 0, 255); //map value for coloring
    tile_color = color(255, 255, 255, cost2color); 

    particle_size = int(cost*0.5); 

    if (cost>0) { //if the cell is positive, create particles
      if (particles.size() < particle_size) { //adding particle if there are less than desired particle size
        PVector ploc = new PVector(x + random(size), y + random(size) );
        particles.add(new Particle(ploc, 0.5, 0.02)); //location, max speed, max force(for steering)
      }
    }
    //run and remove particles
    for (Iterator<Particle> pit = particles.iterator(); pit.hasNext();) {
      Particle p = pit.next();
      p.run();
      p.follow(f);         
      if (p.dead) {
        pit.remove();
      }
    }
  }
  void display() {

    for (Particle p: particles) {
      p.display();
    }
    //draw tile when hit space
    if (key==' ') {
      if (isGoal) {
        fill(255, 0, 255);
      } else {
        fill(tile_color);
      }
      pushStyle();
      stroke(255);
      strokeWeight(0.5);
      rect(x, y, size, size);
      popStyle();
    }

    if (key=='v') {  
      //draw line
      stroke(255);
      line((x+size*0.5), (y+size*0.5), (x+size*0.5)+direction.x*4, (y+size*0.5)+direction.y*4);
      noStroke();
    }
    if (Dbugmode) {
      pushStyle();
      fill(0);
      textSize(size/10);
      text(id, x+size/2, y+size/2);
      text(int(cost), x+10, y+10);
      //text("("+direction.x+","+direction.y+")",x+size/2-10, y+size/2);
      popStyle();
    }
  }
}
