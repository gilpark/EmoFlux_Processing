import de.fhpotsdam.unfolding.*;
import de.fhpotsdam.unfolding.geo.*;
import de.fhpotsdam.unfolding.geo.Location;

import de.fhpotsdam.unfolding.utils.*;
import de.fhpotsdam.unfolding.providers.*;
import de.fhpotsdam.unfolding.marker.*;
import de.fhpotsdam.unfolding.data.*;

import java.util.*;
import java.util.Map;

import peasy.*;

PeasyCam cam;

ArrayList<Test_input> ilist;
int input_range = 15; //range for mapping color and sentiment value 

PVector loc;
FluxSystem flux;
Getstream tstream;

boolean debug = false;
PImage boundbox, back;
PShader blur;
UnfoldingMap map;

void setup() {

  frameRate(30);
  size(1280, 720, P3D);
  //  size(800, 600, P3D);
  //cam
  //  cam = new PeasyCam(this,width/2,height/2,0, 600);
  //  cam.setMinimumDistance(50);
  //  cam.setMaximumDistance(700);

  boundbox = loadImage("bound.gif");
  back = loadImage("bg.gif");

  String mbTilesString = sketchPath("data/emoflux.mbtiles");

  flux = new FluxSystem(10);
  flux.init();
  loc = new PVector(0, 0); // for test input
  ilist = new ArrayList<Test_input>(); //input list
  tstream = new Getstream(); //initialize twitter stream.
  tstream.run();

  map = new UnfoldingMap(this, new MBTilesMapProvider(mbTilesString));
  map.zoomAndPanTo(new Location(38.8910, -96.5039), 4); //-96.5039,38.8910,5
  //colorMode(HSB, 360, 100, 100);  

  background(0);
  noStroke();
  //imageMode(CENTER);
}

void update() {

  flux.update();
}
void draw() { 
  //filter(blur);  
  update();

  if (key==' ')background(0);
  if (key=='d')debug = !debug;

  fill(0, 8);
  rect(0, 0, width, height);
 pushMatrix();
  translate(10, 10, 100);
  flux.display();


  map.draw();
  //image(back, width/2, height/2);
  popMatrix();

  fill(255);
  textSize(10);
  text(frameRate, width-30, 20);
}

void mouseReleased() {
  PVector test = new PVector(mouseX, mouseY);
  flux.testinput(test, random(-10, 10));
}

