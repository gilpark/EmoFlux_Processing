import de.fhpotsdam.unfolding.*;
import de.fhpotsdam.unfolding.geo.*;
import de.fhpotsdam.unfolding.geo.Location;

import de.fhpotsdam.unfolding.utils.*;
import de.fhpotsdam.unfolding.providers.*;
import de.fhpotsdam.unfolding.marker.*;
import de.fhpotsdam.unfolding.data.*;

import java.util.*;
import java.util.Map;

ArrayList<Test_input> ilist;
int input_range = 10; //range for mapping color and sentiment value 

PVector loc;
FluxSystem flux;
Getstream tstream;

boolean debug = true;
boolean dots = true;
boolean ran_input = false;
PImage img, back;
PShader blur;
UnfoldingMap map;

void setup() {
  
  //frameRate(30);
  size(1200, 600, P3D);
  // background(255);
  img = loadImage("texture4.png");
  back = loadImage("bg.gif");
  String mbTilesString = sketchPath("data/esm.mbtiles");
  //blur = loadShader("blur.glsl");
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
  imageMode(CENTER);
}

void update() {

  flux.update();
}
void draw() { 
  //filter(blur);  
  update();
  //println(frameRate);
  //blendMode(LIGHTEST);
  if (key==' ')background(0);


  fill(0, 5);
  rect(0, 0, width, height);

  flux.display();

  if (dots)
    for (int i=0; i < ilist.size (); i++) {
      Test_input a = ilist.get(i);  
      // a.display();
    }
  //map.draw();
  image(back,width/2,height/2);
}


