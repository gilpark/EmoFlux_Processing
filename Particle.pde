// Flow Field Following
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code, Spring 2011
float colorStart =  180;               // Starting dregee of color range in HSB Mode (0-360)
float colorRange =  160;             // color range / can also be negative

class Particle {

  // The usual stuff
  PVector location;
  PVector velocity;
  PVector[] history;

  PVector acceleration;
  float r;
  float maxforce;    // Maximum steering force
  float maxspeed;    // Maximum speed
  int age, age2;
  boolean dead = false;
  color ran;
  int history_counter;
  float dim;
  float stroke;
  
  Particle(PVector l, float ms, float mf) {
    location = l.get();
    r = 3.0;
    maxspeed = ms;
    maxforce = mf;
    acceleration = new PVector(0, 0);
    velocity = new PVector(0, 0);
    age =(int)random(80, 230); //1sec
    age2 = age;
    ran = color(random(255), random(255), random(255));
    history = new PVector[10];
    dim = 20;
    //println("P");
  }

  public void run() {
    update();
    borders();
    history[history_counter] = location;
    history_counter++;
    history_counter %=10;
    if (age < age2/2 && location.dist(history[0]) < 3)dead = true;
//    if (location.dist(history[0]) > 0.1) {
//      println("asdf");
//      dim=200;
//    }
  }

  // Implementing Reynolds' flow field following algorithm
  // http://www.red3d.com/cwr/steer/FlowFollow.html
  void follow(FluxSystem flow) {
    // What is the vector at that spot in the flow field?
    PVector desired = flow.lookup(location);

    // Scale it up by maxspeed
    //desired.mult(maxspeed);
    // Steering is desired minus velocity
    PVector steer;
    PVector pre_desired;

    if (desired.dist(new PVector(0, 0, 0))!=0) {
      pre_desired = desired;
      steer = PVector.sub(desired, velocity);

      steer.limit(maxforce);  // Limit to maximum steering force
      applyForce(steer);
    }
  }

  void applyForce(PVector force) {
    // We could add mass here if we want A = F / M
    acceleration.add(force);
  }

  // Method to update location
  void update() {
    // Update velocity
    velocity.add(acceleration);
    // Limit speed
    velocity.limit(maxspeed);

    location.add(velocity);
    // Reset accelertion to 0 each cycle
    acceleration.mult(0);

    if (age < 0 ) dead = !dead;
    age--;

    //velocity history
  }
  void display() {
   stroke = map(age,0,age2,3,0);
    strokeWeight(stroke);
    stroke(r+colorStart, r+colorStart, r+colorStart, dim);
    r++;
    r %=360;
    point(location.x, location.y);
    noStroke();
    //    fill(255, 60);
    //
    //    ellipse(location.x, location.y, 0.5, 0.5);
  }

  // Wraparound
  void borders() {
    if (location.x <0 ||location.y <0||location.x > width||location.y > height) dead=true;
  }
}

