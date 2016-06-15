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
ArrayList<TStatus> statusList;

int input_range = 15; //range for mapping color and sentiment value 
int percentage;
float posi, nega;
float total;
float transition;

PVector loc;
FluxSystem flux;
Getstream tstream;
PFont font;
boolean debug, wind, dot = false;
PImage boundbox, back, bar;
PShader blur;
UnfoldingMap map;

float fade;
void setup() {

  frameRate(30);
  //size(1280, 720, P3D);
  size(1920, 1080, P3D);
  boundbox = loadImage("bound2.png");
  back = loadImage("back_color.png");
  bar = loadImage("bar.png");
  String mbTilesString = sketchPath("data/emoflux.mbtiles");

  flux = new FluxSystem(15);
  flux.init();
  loc = new PVector(0, 0); // for test input
  ilist = new ArrayList<Test_input>(); //input list
  statusList = new ArrayList<TStatus>();
  tstream = new Getstream(); //initialize twitter stream.
  tstream.run();

  map = new UnfoldingMap(this, new MBTilesMapProvider(mbTilesString));
  //map.zoomAndPanTo(new Location(38.8910, -96.5039), 4); //-96.5039,38.8910,5
  map.zoomAndPanTo(new Location(38.8910, -96.5039), 5);
  background(0);
  noStroke();
}

void update() {
  //  if(posi !=0)
  //  percentage=(posi/total)*100;
  //  println(percentage);
  flux.update();
}
void draw() { 
  println(map(sin(transition), -1, 1, 3.5, 9.5)); 
  update();

  if (key==' ')background(0);
  if (key=='d')debug = !debug;
  transition +=.012;


  //println(map(sin(transition), -1, 1, 0, 10));
  //  fill(0, map(sin(transition), -1, 1, 0, 10));  
  //  rect(0, 0, width, height);
  if(wind){
    tint(255, 255, 255, map(sin(transition), -1, 1, 3.5, 9.5));
//        tint(255, 255, 255, fade);

  }
  image(back, 0, 0);
  if(wind)flux.display();
  tint(255, 255, 255);
  map.draw();

  //LEGEND
  textAlign(RIGHT, CENTER);
  fill(255);

  int legend_x = (int)(width*.75);
  int legend_y = (int)(height*.75);

  font = loadFont("HelveticaNeue-Thin-8.vlw");
  textFont(font, 8);
  //textSize(10);
  text("HAPPYNESS", legend_x-textWidth("SADNESS")/2, legend_y);
  text("SADNESS", legend_x-textWidth("SADNESS")/2, legend_y + 10);

  fill(255, 30);
  rect(legend_x - 10, legend_y-1, 60, 4);
  rect(legend_x - 10, legend_y+9, 60, 4);
  rect(legend_x - 10, legend_y-1, map((posi/total)*100, 0, 100, 0, 60), 4);
  rect(legend_x - 10, legend_y + 9, map((nega/total)*100, 0, 100, 0, 60), 4); 
  fill(255);

  text(date, legend_x + 50, legend_y + 23);
  //text(frameRate, width-30, 20); 

  //saveFrame("frames/######.png"); 

  if (dot) {
    image(bar, legend_x-68, legend_y +30);
    for (int i = statusList.size ()-1; i >= 0; i-- ) {
      TStatus tweet = statusList.get(i);
      if (tweet.age < 0)statusList.remove(tweet);

      tweet.display();
    }
    for (int i = statusList.size ()-1; i >= 0; i-- ) {
      TStatus tweet = statusList.get(i);
      if (tweet.onMouseOver(mouseX, mouseY)) {
        break; // break the loop if a mouse over is detected, so it doesn't fire on overlapping tweets
      }
    }
  }
}
void keyPressed() {
  if (key == '1') {
    dot = !dot;
    println(dot);
  }
  if (key == '2') {
    wind = !wind;
  }
  if (key == 'o'){
    fade +=0.2;
  }
  if(key == 'l'){
    fade -=0.2;
  }
  println(fade);
}
void mouseReleased() {
  PVector test = new PVector(mouseX, mouseY);
  flux.testinput(test, random(-10, 10));
}

