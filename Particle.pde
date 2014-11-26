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
  int age;
  boolean dead = false;
  color ran;
  int history_counter;
  float dim;

  Particle(PVector l, float ms, float mf) {
    location = l.get();
    r = 3.0;
    maxspeed = ms;
    maxforce = mf;
    acceleration = new PVector(0, 0);
    velocity = new PVector(0, 0);
    age = (int)random(50, 200); //1sec
    ran = color(random(255), random(255), random(255));
    history = new PVector[100];
    dim = 100;
    //println("P");
  }

  public void run() {
    update();
    borders();
    // display();
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
      dim++;

      steer.limit(maxforce);  // Limit to maximum steering force
      applyForce(steer);
    }
    if (desired.dist(new PVector(0, 0, 0))==0) {
      dim-=1.5;
    }

    //    history_counter++;
    //    history_counter %=100;
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

    if (velocity.mag() < 20 && age < 70)dead=true;
    age--;
    if (age < 0 ) dead = !dead;

    //velocity history
  }
  void display() {
    pushMatrix();
    translate(location.x, location.y);
    //    fill(255);
    //    ellipse(0, 0, 1, 1);
    strokeWeight(0.5);

    stroke(r+colorStart, r+colorStart, r+colorStart,100);
    r++;
    r %=360;
    //stroke(ran,dim);
    point(0, 0);
    popMatrix();
  }

  // Wraparound
  void borders() {
    if (location.x <0 ||location.y <0||location.x > width||location.y > height) dead=true;
  }
}

