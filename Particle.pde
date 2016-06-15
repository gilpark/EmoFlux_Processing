// Flow Field Following
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code, Spring 2011
float colorStart =  180;               // Starting dregee of color range in HSB Mode (0-360)
float colorRange =  160;             // color range / can also be negative

color startColor = #e895b3;
color endColor = #e8e395;
//color startColor2 = #66ff00;
//color endColor2 = #9600ff;
color startColor2 = color(255,255,255);//#66ff00;
color endColor2 = color(0,0,0);

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
    history = new PVector[2];
    dim = 0;
    //println("P");
  }

  public void run() {
    update();
    borders();

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
      //pre_desired = desired;
      steer = PVector.sub(desired, velocity);

      steer.limit(maxforce);  // Limit to maximum steering force
      applyForce(steer);
      dim = 10;
    }
    if (desired.mag()<1) {
      dim -=0.5;
      //dead=true;
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
    stroke = map(age, 0, age2, 2, 0);
    strokeWeight(stroke);
    //stroke(r+colorStart, r+colorStart, r+colorStart, dim);
    color pointcolor = lerpColor(startColor,endColor,map(age,age2,0,0,1));
    stroke(red(pointcolor),green(pointcolor),blue(pointcolor),dim);
    
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
    if (location.x <100 ||location.y <100||location.x > width-50||location.y > height-50) dead=true;
  }
}

