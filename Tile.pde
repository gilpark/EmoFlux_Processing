class Tile {

  int id;
  int size;
  float cost;
  float cost2color;
  boolean isGoal; //
  boolean debug;
  boolean isPassible;
  float x, y;
  PVector direction;
  FluxSystem f;
  ArrayList<Particle> particles;
  color tile_color;

  Tile(float _x, float _y, int _id, int _r, FluxSystem _f) {
    x = _x;
    y = _y;
    id = _id;
    f = _f;
    size = _r;
    isGoal = false;
    debug = false;
    isPassible = true;
    cost = 0;
    direction = new PVector(0, 0);
    //println(x);
    particles = new ArrayList<Particle>();
    //    for (int i=0; i<1; i++) {
    //      PVector ploc = new PVector(x + random(size), y + random(size) );
    //      particles.add(new Particle(ploc, 1.5, 0.1));
    //    }
  }

  void reset() {
    cost = 0;
    isGoal = false;
    debug = false;
    isPassible = true;
  }

  void update() {
    if (cost<1) {
      if (particles.size()<5) {//adding particle if there are less than 5 in the cell
        PVector ploc = new PVector(x + random(size), y + random(size) );
        particles.add(new Particle(ploc, 0.5, 0.02));
      }
    }
    //println(particles.size());
    for (int i= particles.size ()-1; i>=0; i--) {
      Particle p = particles.get(i);
      p.run();
      p.follow(f);         
      if (p.dead) {
        particles.remove(i);
      }
    }

    cost2color = map(cost, -10, 10, 0, 255);
    tile_color = color(255, 255, 255, cost2color);
  }
  void display() {
    if (key==' ') {
      //draw tile
      if (isGoal) {
        fill(255, 0, 255);
        //      } else if (cost==0) {
        //        fill(0);
      } else {
        fill(tile_color);
      }  
      noStroke();
      rect(x, y, size, size);
      //fill(0);
      //text(id, x+size/2, y+size/2);
      fill(255);
      // text(int(cost), x+10, y+10);
      textSize(10);
      //text("("+direction.x+","+direction.y+")",x+size/2-10, y+size/2);
    }
    for (int i=0; i<particles.size (); i++) {
      Particle p = particles.get(i);
      if (key=='p')

        p.display();
    }

    if (key=='v') {  
      //draw line
      stroke(255);
      line((x+size*0.5), (y+size*0.5), (x+size*0.5)+direction.x*4, (y+size*0.5)+direction.y*4);
      noStroke();
    }
  }
}

