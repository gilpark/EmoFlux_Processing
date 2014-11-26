import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import de.fhpotsdam.unfolding.*; 
import de.fhpotsdam.unfolding.geo.*; 
import de.fhpotsdam.unfolding.geo.Location; 
import de.fhpotsdam.unfolding.utils.*; 
import de.fhpotsdam.unfolding.providers.*; 
import de.fhpotsdam.unfolding.marker.*; 
import de.fhpotsdam.unfolding.data.*; 
import java.util.*; 
import java.util.Map; 
import twitter4j.util.*; 
import twitter4j.*; 
import twitter4j.management.*; 
import twitter4j.api.*; 
import twitter4j.conf.*; 
import twitter4j.json.*; 
import twitter4j.auth.*; 
import java.util.regex.*; 

import org.sqlite.*; 
import org.sqlite.javax.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Emotion_Stream_Map_2 extends PApplet {













ArrayList<Test_input> ilist;

PVector loc;
FlowField flowfield;
Getstream tstream;

boolean debug = true;
boolean dots = true;
boolean ran_input = false;


UnfoldingMap map;

public void setup() {
  size(800, 800,P3D);
  background(255);
//  pg = createGraphics(width,height);
  String mbTilesString = sketchPath("data/esm.mbtiles");

  flowfield = new FlowField(40);
  loc = new PVector(0, 0); // for test input
  ilist = new ArrayList<Test_input>(); //input list
  tstream = new Getstream(); //initialize twitter stream.
  //tstream.run();

  //map::::map1: satlelite maps
  //map = new UnfoldingMap(this, new Microsoft.AerialProvider());
  //map.setZoomRange(12,16);
  //map.zoomAndPanTo(new Location(38.8910, -96.5039), 5); //-96.5039,38.8910,5
  //map.setTweening(true);
  map = new UnfoldingMap(this, new MBTilesMapProvider(mbTilesString));
  map.zoomAndPanTo(new Location(38.8910f, -96.5039f), 5); //-96.5039,38.8910,5


  noStroke();
}

public void draw() { 

  fill(255,20);

  rect(0,0,width,height);
  
  flowfield.display();
  
  if(dots)
  for (int i=0; i < ilist.size (); i++) {
    Test_input a = ilist.get(i);  
    a.display();
  }

  //  map.draw(); // base map

}

public void keyPressed() {
  if (key == ' ') {
    debug = !debug;
  }
  if (key == 'r') {
    ran_input = !ran_input;
  }
  if (key == 'd'){
  dots = !dots;
  }
}

 public void mousePressed() {
   float ran = random(0, 255);
   loc.set(mouseX, mouseY);
   ilist.add(new Test_input(loc, ran));
   flowfield.lookup_add_unit(loc, ran);
 }

class FlowField {

  PVector[][] field;
  int cols, rows;
  int resolution;
  Grid_unit[][] units;
  int unit_id;
  //PVector unit_index;
  PVector ran_loc = new PVector(0, 0);


  FlowField(int r) {
    resolution = r;
    cols = width/resolution;
    rows = height/resolution;
    field = new PVector[cols][rows];
    units = new Grid_unit[cols][rows];



    init();
    //rectMode(CENTER);
  }
  //setting up vector grid and unit grid.
  public void init() {

    for (int i = 0; i < cols; i++) {

      for (int j = 0; j < rows; j++) {

        field[i][j] = new PVector(0, 0);

        units[i][j] = new Grid_unit(new PVector(i*resolution, j*resolution), resolution, unit_id, this, 0);
        unit_id++; //unit counter
      }
    }
  }

  public void display() {


    for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {

        if (debug) { 
          units[i][j].display();
          drawVector(field[i][j], i*resolution, j*resolution, resolution+2);
        } 
        if (!debug)
          units[i][j].update_particle();

        noFill();
        //update vector heading
        if (frameCount%60==0)
          compute_units(units[i][j].id, i, j);

        if (ran_input && frameCount%30==0) {
          float ran = random(0, 255);
          ran_loc.set(random(width), random(height));
          lookup_add_unit(ran_loc, ran);
        }
      }
    }
  }



  public void drawVector(PVector v, float x, float y, float scayl) {

    pushMatrix();
    //float arrowsize = 20;

    translate(x+resolution*0.5f, y+resolution*0.5f);
    rotate(v.heading2D());
    stroke(255);
    line(0, 0, resolution/2, 0);
    popMatrix();
  }

  public PVector lookup(PVector lookup) {

    int column = PApplet.parseInt(constrain(lookup.x/resolution, 0, cols-1));
    int row = PApplet.parseInt(constrain(lookup.y/resolution, 0, rows-1));
    return field[column][row].get();
  }

  public void addtweet(GeoLocation _loc, String msg) {
    Location loc = new Location(_loc.getLatitude(), _loc.getLongitude());
    //Location timeLoc = datamap.getLocation(Pos.x, Pos.y);
    ScreenPosition newloc  = map.getScreenPosition(loc);
    PVector pos = new PVector(newloc.x, newloc.y);

    lookup_add_unit(pos, Sentiment(msg));
    ilist.add(new Test_input(pos, Sentiment(msg)));
  }

  public void lookup_add_unit(PVector lookup, float val) {
    int id = 0;
    int column = PApplet.parseInt(constrain(lookup.x/resolution, 0, cols-1));
    int row = PApplet.parseInt(constrain(lookup.y/resolution, 0, rows-1));

    units[column][row].addinputs(val);
    compute_units(units[column][row].id, column, row);
  }

  public void compute_units(int id, int col, int row) {

    int col_index = id/rows;
    int row_index = id%rows;
    Grid_unit largest = units[col][row];
    Grid_unit smallest = units[col][row];
    for (int i=col_index -1; i < col_index +2; i++) {

      if (i < 0 || i > cols-1) continue;

      for (int j=row_index -1; j < row_index +2; j++) {
        if (j < 0 || j > rows-1) continue;
        //if (col == i && row == j) continue; //skip to check itself
        //println(id+"'s neighbor : "+units[i][j].id);
        Grid_unit unit = units[col][row]; //one that we r evaluating
        Grid_unit neighbor = units[i][j];  //others
        Grid_unit max_unit = largest;
        Grid_unit min_unit = smallest;
        smallest = MIN(min_unit, neighbor);
        largest = MAX(max_unit, neighbor);  
        //println("max one is "+largest.id);
        text(unit.id,loc.x, loc.y);

        PVector unit_loc =  units[col][row].loc.get();
        PVector largest_loc = largest.loc.get();
        PVector result_direction = new PVector(0, 0);

        float max_speed = map(largest.unit_color - unit.unit_color, 0, 255, 0, 10);
        unit.max_speed = max_speed; 
        largest_loc.sub(unit_loc);
        largest_loc.normalize();
        result_direction = largest_loc;
       // if (frameCount%30==0)
          field[col][row] = result_direction;

      }
    }
  }

  public Grid_unit MAX(Grid_unit val_1, Grid_unit val_2 ) {
    //println(val_1.id+" is comparing to "+val_2.id);
    if (val_1.unit_color < val_2.unit_color) { //if there is brighter one in neighbors
      //val_2.unit_color -= 0.0001;
      if(val_2.unit_color > 255*0.5f)
      val_1.unit_color = val_1.addinputs2(val_2.unit_color) ;
      //val_1.unit_color = color(255,0,0);
      return val_2;
    } else if (val_1.unit_color > val_2.unit_color) { //if the centerone is brighter one
      //val_2.unit_color = val_2.addinputs2(val_1.unit_color*0.80) ;
      //val_1.unit_color -= 0.0001;
      return val_1;
    } else
      return val_1;
  }

  public Grid_unit MIN(Grid_unit val_1, Grid_unit val_2 ) {
    //println(val_1.id+" is comparing to "+val_2.id);
    if (val_1.unit_color < val_2.unit_color) { //if there is brighter one in neighbors
      //val_2.unit_color = val_1.unit_color*0.10;
     // val_1.unit_color += 0.0001 ;
      //val_1.unit_color = val_1.addinputs2(val_2.unit_color) ;

      return val_1;
    } else if (val_1.unit_color > val_2.unit_color) { //if the centerone is brighter one
     // val_2.unit_color = val_1.unit_color*0.20 ;
      //val_1.unit_color += 0.0001;
      if(val_2.unit_color < 255*0.5f)
      val_1.unit_color = val_1.addinputs2(val_2.unit_color) ;

      return val_2;
    } else
      return val_1;
  }
}









class Getstream
{	
  ///////////////////// set your twitter dev info ////////////////////////////////
  /// This is where you enter your Oauth info
  String OAuthConsumerKey = "zaO66kuC9onpl3JT1mJHmvJTV";
  String OAuthConsumerSecret = "LKuBOhkrGBir7QYsh8MB53CFyMfRJr4o3TWwzlP3Sx10hb1vbu";
  // This is where you enter your Access Token info
  String AccessToken = "2484618828-nwlFhfbygQTXb2mX9S4tON8AINzHMF122YA4ZMM";
  String AccessTokenSecret = "XnePyCIxhcaqNeoxJ5CaOmPrhEMvRWRxAnWH3r5slrYJT";





  // String OAuthConsumerKey = "hlTBBoYudhL44hHwegUezQ";
  // String OAuthConsumerSecret = "k0AmVsQ7e8dvu98h5POxWHoXtYL88YOXP0UzhpUV98";
  // // This is where you enter your Access Token info
  // String AccessToken = "1299643790-OEFCavotvw1G8gUYQrEG5TSoxHeTp2GsBiV5zlJ";
  // String AccessTokenSecret = "wM9ov7ajQcUUkdgIGm4XMKtaV5zB5vGMky16sbkQlAI";

  public AccessToken loadAccessToken() 
  
  {
    return new AccessToken(AccessToken, AccessTokenSecret);
  }
  ////////////////////////////////////////////////////////////////////////////////
  //initialize twitter stream	
  TwitterStream twitter = new TwitterStreamFactory().getInstance();


  

  double[][] loc = null;
  String[] keywords = {"happy","sad",
  };

  Getstream()  
  {  
    connectTwitter();
    twitter.addListener(listener);
  }


  // Initial connection
  public void connectTwitter() 
  {
    twitter.setOAuthConsumer(OAuthConsumerKey, OAuthConsumerSecret);
    AccessToken accessToken = loadAccessToken();
    twitter.setOAuthAccessToken(accessToken);
  }

  //search tweets
  public void run()
  {    
    twitter.sample();
    //getbox(41.880, -87.636);
    //search(keywords,loc);
    //loc = {{-129.9023, 22.7964}, {-64.5996, 51.1518}};
    //search(loc);

  }


  public void getbox(double lat, double lon){ //setting bounding box


    double lon1 = lon - .8f;
    double lon2 = lon + .8f;
    double lat1 = lat - .8f;
    double lat2 = lat + .8f;
    //double box[][] = {{lon1, lat1}, {lon2, lat2}};
     double box[][] ={{-129.9023f, 22.7964f}, {-64.5996f, 51.1518f}};
    loc = box;
    println("1-lat,lon : "+ lat1+ " , "+lon1+" 2-lat,lon : "+ lat2+ " , "+lon2);
  }

  public void search(String[] keywords, double[][] loc)
  {
      
      FilterQuery query = new FilterQuery();
      query.track(keywords);
      query.locations(loc);
      twitter.filter(query);
  }
  public void search(double[][] loc)
  {
      
      FilterQuery query = new FilterQuery();
      query.locations(loc);
      twitter.filter(query);
  }
  public void shutdown()
  {
    twitter.shutdown();
  }

  // This listens for new tweet
  StatusListener listener = new StatusListener() 
  {

    public void onStatus(Status status) {
      //more info -> http://twitter4j.org/javadoc/twitter4j/Status.html
      String name = status.getUser().getScreenName();
      String msg = status.getText();
      GeoLocation loc = status.getGeoLocation();
      Place place = status.getPlace();
      Date time = status.getCreatedAt();
      String lang = status.getLang();
      
      //fillter only english tweets
      if(lang.equals("en")){

      //@TODO
      flowfield.addtweet(loc, msg);

      //geolocation obj = lat, lon
      // if(ecosystem.particle_list.size()<120 || ecosystem.posparticle_list.size()<10)
      // ecosystem.addParticle(msg);

      }
    }

    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
      //System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
    }
    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
      // System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
    }
    public void onScrubGeo(long userId, long upToStatusId) {
      //System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
    }
    public void onStallWarning(StallWarning warning) {
      //System.out.println("Got stall warning:" + warning);
    }

    public void onException(Exception ex) {
      //ex.printStackTrace();
    }
  };
}


class Grid_unit {
  float unit_color;
  float size;
  PVector loc;
  int id;
  float unit_val;
  FlowField flow;
  float max_speed;
  
  ArrayList<Vehicle> vehicles;

  Grid_unit(PVector _loc, float _size, int _id, FlowField f,float _max_speed) {
    unit_color = 255*0.5f;
    loc = _loc;
    size = _size;
    id = _id;
    flow = f;
    max_speed = _max_speed;
    vehicles = new ArrayList<Vehicle>();
      for (int i = 0; i < 10; i++) {
        vehicles.add(new Vehicle(new PVector(loc.x+random(size), loc.y+random(size)), max_speed, random(0.005f, 0.01f)));
      }
  }
 
  public void update_particle() {


    Iterator<Vehicle> it = vehicles.iterator();
    while (it.hasNext ()) {

      Vehicle v = (Vehicle) it.next();
      v.follow(flow);
      v.run();
      if (v.dead) {
        it.remove();

      }
    }
    if(vehicles.size()<10)vehicles.add(new Vehicle(new PVector(loc.x+random(size), loc.y+random(size)), max_speed, random(0.005f, 0.01f)));

  }

  public void display() {
    noStroke();
    fill(unit_color);
    rect(loc.x, loc.y, size, size);
    fill(0);
    textSize(10);
    //text(id+":"+(int)unit_color, loc.x+20, loc.y+40);

  }



  public void addinputs(float input) {
    unit_color = (unit_color+input)*0.5f;
    //unit_color = unit_color+ input;
    unit_val = unit_color;
  }
    public float addinputs2(float input) {
    return unit_color = (unit_color+input)*0.5f;

  }
}

class Test_input {
  PVector pos;
  float val;
  Test_input(PVector _pos, float _val) {
    val = _val;
    pos = new PVector(_pos.x, _pos.y);
  } 
  public void display() {
    //fill(val);
    stroke(255,0,0,200);
    rect(pos.x, pos.y, 4, 4);
    //ellipse(pos.x, pos.y, 2, 2) ;

    noStroke(); 
  }
}

// Flow Field Following
// Daniel Shiffman <http://www.shiffman.net>
// The Nature of Code, Spring 2011

class Vehicle {

  // The usual stuff
  PVector location;
  PVector velocity;
  PVector acceleration;
  float r;
  float maxforce;    // Maximum steering force
  float maxspeed;    // Maximum speed
  int age;
  boolean dead = false;
  
    Vehicle(PVector l, float ms, float mf) {
    location = l.get();
    r = 3.0f;
    maxspeed = ms;
    maxforce = mf;
    acceleration = new PVector(0, 0);
    velocity = new PVector(0, 0);
    age = (int)random(100,200); //1sec
  }

  public void run() {
    update();
    borders();
    display();
  }


  // Implementing Reynolds' flow field following algorithm
  // http://www.red3d.com/cwr/steer/FlowFollow.html
  public void follow(FlowField flow) {
    // What is the vector at that spot in the flow field?
    PVector desired = flow.lookup(location);
    // Scale it up by maxspeed
    
    desired.mult(maxspeed);
    // Steering is desired minus velocity
    PVector steer = PVector.sub(desired, velocity);
    steer.limit(maxforce);  // Limit to maximum steering force
    applyForce(steer);
  }

  public void applyForce(PVector force) {
    // We could add mass here if we want A = F / M
    acceleration.add(force);
  }

  // Method to update location
  public void update() {
    // Update velocity
    velocity.add(acceleration);
    // Limit speed
    velocity.limit(maxspeed);
    location.add(velocity);
    // Reset accelertion to 0 each cycle
    acceleration.mult(0);
    age--;
    if(age < 0) dead = !dead;
  }

  public void display() {
//    // Draw a triangle rotated in the direction of velocity


    float theta = velocity.heading2D() + radians(90);
    
    //fill(175);
    stroke(0);
    pushMatrix();
    translate(location.x, location.y);
    point(0,0);
    rotate(theta);
//    beginShape(TRIANGLES);
//    vertex(0, -r*2);
//    vertex(-r, r*2);
//    vertex(r, r*2);
//    endShape();
    popMatrix();
  
  }

  // Wraparound
  public void borders() {
    if (location.x < -r) location.x = width+r;
    if (location.y < -r) location.y = height+r;
    if (location.x > width+r) location.x = -r;
    if (location.y > height+r) location.y = -r;
  }
}


float s_max=0, s_min=0;
//lexicon lists
String negations = 
"(never|nothing|nowhere|no one|none|not|havent|hasnt|hadnt|cant|couldnt|shouldnt|wont|wouldnt|dont|doesnt|didnt|isnt|arent|aint)";
String pos=
"(a+|abound|abounds|abundance|abundant|accessable|accessible|acclaim|acclaimed|acclamation|accolade|accolades|accommodative|accomodative|accomplish|accomplished|accomplishment|accomplishments|accurate|accurately|achievable|achievement|achievements|achievible|acumen|adaptable|adaptive|adequate|adjustable|admirable|admirably|admiration|admire|admirer|admiring|admiringly|adorable|adore|adored|adorer|adoring|adoringly|adroit|adroitly|adulate|adulation|adulatory|advanced|advantage|advantageous|advantageously|advantages|adventuresome|adventurous|advocate|advocated|advocates|affability|affable|affably|affectation|affection|affectionate|affinity|affirm|affirmation|affirmative|affluence|affluent|afford|affordable|affordably|afordable|agile|agilely|agility|agreeable|agreeableness|agreeably|all-around|alluring|alluringly|altruistic|altruistically|amaze|amazed|amazement|amazes|amazing|amazingly|ambitious|ambitiously|ameliorate|amenable|amenity|amiability|amiabily|amiable|amicability|amicable|amicably|amity|ample|amply|amuse|amusing|amusingly|angel|angelic|apotheosis|appeal|appealing|applaud|appreciable|appreciate|appreciated|appreciates|appreciative|appreciatively|appropriate|approval|approve|ardent|ardently|ardor|articulate|aspiration|aspirations|aspire|assurance|assurances|assure|assuredly|assuring|astonish|astonished|astonishing|astonishingly|astonishment|astound|astounded|astounding|astoundingly|astutely|attentive|attraction|attractive|attractively|attune|audible|audibly|auspicious|authentic|authoritative|autonomous|available|aver|avid|avidly|award|awarded|awards|awe|awed|awesome|awesomely|awesomeness|awestruck|awsome|backbone|balanced|bargain|beauteous|beautiful|beautifullly|beautifully|beautify|beauty|beckon|beckoned|beckoning|beckons|believable|believeable|beloved|benefactor|beneficent|beneficial|beneficially|beneficiary|benefit|benefits|benevolence|benevolent|benifits|best|best-known|best-performing|best-selling|better|better-known|better-than-expected|beutifully|blameless|bless|blessing|bliss|blissful|blissfully|blithe|blockbuster|bloom|blossom|bolster|bonny|bonus|bonuses|boom|booming|boost|boundless|bountiful|brainiest|brainy|brand-new|brave|bravery|bravo|breakthrough|breakthroughs|breathlessness|breathtaking|breathtakingly|breeze|bright|brighten|brighter|brightest|brilliance|brilliances|brilliant|brilliantly|brisk|brotherly|bullish|buoyant|cajole|calm|calming|calmness|capability|capable|capably|captivate|captivating|carefree|cashback|cashbacks|catchy|celebrate|celebrated|celebration|celebratory|champ|champion|charisma|charismatic|charitable|charm|charming|charmingly|chaste|cheaper|cheapest|cheer|cheerful|cheery|cherish|cherished|cherub|chic|chivalrous|chivalry|civility|civilize|clarity|classic|classy|clean|cleaner|cleanest|cleanliness|cleanly|clear|clear-cut|cleared|clearer|clearly|clears|clever|cleverly|cohere|coherence|coherent|cohesive|colorful|comely|comfort|comfortable|comfortably|comforting|comfy|commend|commendable|commendably|commitment|commodious|compact|compactly|compassion|compassionate|compatible|competitive|complement|complementary|complemented|complements|compliant|compliment|complimentary|comprehensive|conciliate|conciliatory|concise|confidence|confident|congenial|congratulate|congratulation|congratulations|congratulatory|conscientious|considerate|consistent|consistently|constructive|consummate|contentment|continuity|contrasty|contribution|convenience|convenient|conveniently|convience|convienient|convient|convincing|convincingly|cool|coolest|cooperative|cooperatively|cornerstone|correct|correctly|cost-effective|cost-saving|counter-attack|counter-attacks|courage|courageous|courageously|courageousness|courteous|courtly|covenant|cozy|creative|credence|credible|crisp|crisper|cure|cure-all|cushy|cute|cuteness|danke|danken|daring|daringly|darling|dashing|dauntless|dawn|dazzle|dazzled|dazzling|dead-cheap|dead-on|decency|decent|decisive|decisiveness|dedicated|defeat|defeated|defeating|defeats|defender|deference|deft|deginified|delectable|delicacy|delicate|delicious|delight|delighted|delightful|delightfully|delightfulness|dependable|dependably|deservedly|deserving|desirable|desiring|desirous|destiny|detachable|devout|dexterous|dexterously|dextrous|dignified|dignify|dignity|diligence|diligent|diligently|diplomatic|dirt-cheap|distinction|distinctive|distinguished|diversified|divine|divinely|dominate|dominated|dominates|dote|dotingly|doubtless|dreamland|dumbfounded|dumbfounding|dummy-proof|durable|dynamic|eager|eagerly|eagerness|earnest|earnestly|earnestness|ease|eased|eases|easier|easiest|easiness|easing|easy|easy-to-use|easygoing|ebullience|ebullient|ebulliently|ecenomical|economical|ecstasies|ecstasy|ecstatic|ecstatically|edify|educated|effective|effectively|effectiveness|effectual|efficacious|efficient|efficiently|effortless|effortlessly|effusion|effusive|effusively|effusiveness|elan|elate|elated|elatedly|elation|electrify|elegance|elegant|elegantly|elevate|elite|eloquence|eloquent|eloquently|embolden|eminence|eminent|empathize|empathy|empower|empowerment|enchant|enchanted|enchanting|enchantingly|encourage|encouragement|encouraging|encouragingly|endear|endearing|endorse|endorsed|endorsement|endorses|endorsing|energetic|energize|energy-efficient|energy-saving|engaging|engrossing|enhance|enhanced|enhancement|enhances|enjoy|enjoyable|enjoyably|enjoyed|enjoying|enjoyment|enjoys|enlighten|enlightenment|enliven|ennoble|enough|enrapt|enrapture|enraptured|enrich|enrichment|enterprising|entertain|entertaining|entertains|enthral|enthrall|enthralled|enthuse|enthusiasm|enthusiast|enthusiastic|enthusiastically|entice|enticed|enticing|enticingly|entranced|entrancing|entrust|enviable|enviably|envious|enviously|enviousness|envy|equitable|ergonomical|err-free|erudite|ethical|eulogize|euphoria|euphoric|euphorically|evaluative|evenly|eventful|everlasting|evocative|exalt|exaltation|exalted|exaltedly|exalting|exaltingly|examplar|examplary|excallent|exceed|exceeded|exceeding|exceedingly|exceeds|excel|exceled|excelent|excellant|excelled|excellence|excellency|excellent|excellently|excels|exceptional|exceptionally|excite|excited|excitedly|excitedness|excitement|excites|exciting|excitingly|exellent|exemplar|exemplary|exhilarate|exhilarating|exhilaratingly|exhilaration|exonerate|expansive|expeditiously|expertly|exquisite|exquisitely|extol|extoll|extraordinarily|extraordinary|exuberance|exuberant|exuberantly|exult|exultant|exultation|exultingly|eye-catch|eye-catching|eyecatch|eyecatching|fabulous|fabulously|facilitate|fair|fairly|fairness|faith|faithful|faithfully|faithfulness|fame|famed|famous|famously|fancier|fancinating|fancy|fanfare|fans|fantastic|fantastically|fascinate|fascinating|fascinatingly|fascination|fashionable|fashionably|fast|fast-growing|fast-paced|faster|fastest|fastest-growing|faultless|fav|fave|favor|favorable|favored|favorite|favorited|favour|fearless|fearlessly|feasible|feasibly|feat|feature-rich|fecilitous|feisty|felicitate|felicitous|felicity|fertile|fervent|fervently|fervid|fervidly|fervor|festive|fidelity|fiery|fine|fine-looking|finely|finer|finest|firmer|first-class|first-in-class|first-rate|flashy|flatter|flattering|flatteringly|flawless|flawlessly|flexibility|flexible|flourish|flourishing|fluent|flutter|fond|fondly|fondness|foolproof|foremost|foresight|formidable|fortitude|fortuitous|fortuitously|fortunate|fortunately|fortune|fragrant|free|freed|freedom|freedoms|fresh|fresher|freshest|friendliness|friendly|frolic|frugal|fruitful|ftw|fulfillment|fun|futurestic|futuristic|gaiety|gaily|gain|gained|gainful|gainfully|gaining|gains|gallant|gallantly|galore|geekier|geeky|gem|gems|generosity|generous|generously|genial|genius|gentle|gentlest|genuine|gifted|glad|gladden|gladly|gladness|glamorous|glee|gleeful|gleefully|glimmer|glimmering|glisten|glistening|glitter|glitz|glorify|glorious|gloriously|glory|glow|glowing|glowingly|god-given|god-send|godlike|godsend|gold|golden|good|goodly|goodness|goodwill|goood|gooood|gorgeous|gorgeously|grace|graceful|gracefully|gracious|graciously|graciousness|grand|grandeur|grateful|gratefully|gratification|gratified|gratifies|gratify|gratifying|gratifyingly|gratitude|great|greatest|greatness|grin|groundbreaking|guarantee|guidance|guiltless|gumption|gush|gusto|gutsy|hail|halcyon|hale|hallmark|hallmarks|hallowed|handier|handily|hands-down|handsome|handsomely|handy|happier|happily|happiness|happy|hard-working|hardier|hardy|harmless|harmonious|harmoniously|harmonize|harmony|headway|heal|healthful|healthy|hearten|heartening|heartfelt|heartily|heartwarming|heaven|heavenly|helped|helpful|helping|hero|heroic|heroically|heroine|heroize|heros|high-quality|high-spirited|hilarious|holy|homage|honest|honesty|honor|honorable|honored|honoring|hooray|hopeful|hospitable|hot|hotcake|hotcakes|hottest|hug|humane|humble|humility|humor|humorous|humorously|humour|humourous|ideal|idealize|ideally|idol|idolize|idolized|idyllic|illuminate|illuminati|illuminating|illumine|illustrious|ilu|imaculate|imaginative|immaculate|immaculately|immense|impartial|impartiality|impartially|impassioned|impeccable|impeccably|important|impress|impressed|impresses|impressive|impressively|impressiveness|improve|improved|improvement|improvements|improves|improving|incredible|incredibly|indebted|individualized|indulgence|indulgent|industrious|inestimable|inestimably|inexpensive|infallibility|infallible|infallibly|influential|ingenious|ingeniously|ingenuity|ingenuous|ingenuously|innocuous|innovation|innovative|inpressed|insightful|insightfully|inspiration|inspirational|inspire|inspiring|instantly|instructive|instrumental|integral|integrated|intelligence|intelligent|intelligible|interesting|interests|intimacy|intimate|intricate|intrigue|intriguing|intriguingly|intuitive|invaluable|invaluablely|inventive|invigorate|invigorating|invincibility|invincible|inviolable|inviolate|invulnerable|irreplaceable|irreproachable|irresistible|irresistibly|issue-free|jaw-droping|jaw-dropping|jollify|jolly|jovial|joy|joyful|joyfully|joyous|joyously|jubilant|jubilantly|jubilate|jubilation|jubiliant|judicious|justly|keen|keenly|keenness|kid-friendly|kindliness|kindly|kindness|knowledgeable|kudos|large-capacity|laud|laudable|laudably|lavish|lavishly|law-abiding|lawful|lawfully|lead|leading|leads|lean|led|legendary|leverage|levity|liberate|liberation|liberty|lifesaver|light-hearted|lighter|likable|like|liked|likes|liking|lionhearted|lively|logical|long-lasting|lovable|lovably|love|loved|loveliness|lovely|lover|loves|loving|low-cost|low-price|low-priced|low-risk|lower-priced|loyal|loyalty|lucid|lucidly|luck|luckier|luckiest|luckiness|lucky|lucrative|luminous|lush|luster|lustrous|luxuriant|luxuriate|luxurious|luxuriously|luxury|lyrical|magic|magical|magnanimous|magnanimously|magnificence|magnificent|magnificently|majestic|majesty|manageable|maneuverable|marvel|marveled|marvelled|marvellous|marvelous|marvelously|marvelousness|marvels|master|masterful|masterfully|masterpiece|masterpieces|masters|mastery|matchless|mature|maturely|maturity|meaningful|memorable|merciful|mercifully|mercy|merit|meritorious|merrily|merriment|merriness|merry|mesmerize|mesmerized|mesmerizes|mesmerizing|mesmerizingly|meticulous|meticulously|mightily|mighty|mind-blowing|miracle|miracles|miraculous|miraculously|miraculousness|modern|modest|modesty|momentous|monumental|monumentally|morality|motivated|multi-purpose|navigable|neat|neatest|neatly|nice|nicely|nicer|nicest|nifty|nimble|noble|nobly|noiseless|non-violence|non-violent|notably|noteworthy|nourish|nourishing|nourishment|novelty|nurturing|oasis|obsession|obsessions|obtainable|openly|openness|optimal|optimism|optimistic|opulent|orderly|originality|outdo|outdone|outperform|outperformed|outperforming|outperforms|outshine|outshone|outsmart|outstanding|outstandingly|outstrip|outwit|ovation|overjoyed|overtake|overtaken|overtakes|overtaking|overtook|overture|pain-free|painless|painlessly|palatial|pamper|pampered|pamperedly|pamperedness|pampers|panoramic|paradise|paramount|pardon|passion|passionate|passionately|patience|patient|patiently|patriot|patriotic|peace|peaceable|peaceful|peacefully|peacekeepers|peach|peerless|pep|pepped|pepping|peppy|peps|perfect|perfection|perfectly|permissible|perseverance|persevere|personages|personalized|phenomenal|phenomenally|picturesque|piety|pinnacle|playful|playfully|pleasant|pleasantly|pleased|pleases|pleasing|pleasingly|pleasurable|pleasurably|pleasure|plentiful|pluses|plush|plusses|poetic|poeticize|poignant|poise|poised|polished|polite|politeness|popular|portable|posh|positive|positively|positives|powerful|powerfully|praise|praiseworthy|praising|pre-eminent|precious|precise|precisely|preeminent|prefer|preferable|preferably|prefered|preferes|preferring|prefers|premier|prestige|prestigious|prettily|pretty|priceless|pride|principled|privilege|privileged|prize|proactive|problem-free|problem-solver|prodigious|prodigiously|prodigy|productive|productively|proficient|proficiently|profound|profoundly|profuse|profusion|progress|progressive|prolific|prominence|prominent|promise|promised|promises|promising|promoter|prompt|promptly|proper|properly|propitious|propitiously|pros|prosper|prosperity|prosperous|prospros|protect|protection|protective|proud|proven|proves|providence|proving|prowess|prudence|prudent|prudently|punctual|pure|purify|purposeful|quaint|qualified|qualify|quicker|quiet|quieter|radiance|radiant|rapid|rapport|rapt|rapture|raptureous|raptureously|rapturous|rapturously|rational|razor-sharp|reachable|readable|readily|ready|reaffirm|reaffirmation|realistic|realizable|reasonable|reasonably|reasoned|reassurance|reassure|receptive|reclaim|recomend|recommend|recommendation|recommendations|recommended|reconcile|reconciliation|record-setting|recover|recovery|rectification|rectify|rectifying|redeem|redeeming|redemption|refine|refined|refinement|reform|reformed|reforming|reforms|refresh|refreshed|refreshing|refund|refunded|regal|regally|regard|rejoice|rejoicing|rejoicingly|rejuvenate|rejuvenated|rejuvenating|relaxed|relent|reliable|reliably|relief|relish|remarkable|remarkably|remedy|remission|remunerate|renaissance|renewed|renown|renowned|replaceable|reputable|reputation|resilient|resolute|resound|resounding|resourceful|resourcefulness|respect|respectable|respectful|respectfully|respite|resplendent|responsibly|responsive|restful|restored|restructure|restructured|restructuring|retractable|revel|revelation|revere|reverence|reverent|reverently|revitalize|revival|revive|revives|revolutionary|revolutionize|revolutionized|revolutionizes|reward|rewarding|rewardingly|rich|richer|richly|richness|right|righten|righteous|righteously|righteousness|rightful|rightfully|rightly|rightness|risk-free|robust|rock-star|rock-stars|rockstar|rockstars|romantic|romantically|romanticize|roomier|roomy|rosy|safe|safely|sagacity|sagely|saint|saintliness|saintly|salutary|salute|sane|satisfactorily|satisfactory|satisfied|satisfies|satisfy|satisfying|satisified|saver|savings|savior|savvy|scenic|seamless|seasoned|secure|securely|selective|self-determination|self-respect|self-satisfaction|self-sufficiency|self-sufficient|sensation|sensational|sensationally|sensations|sensible|sensibly|sensitive|serene|serenity|sexy|sharp|sharper|sharpest|shimmering|shimmeringly|shine|shiny|significant|silent|simpler|simplest|simplified|simplifies|simplify|simplifying|sincere|sincerely|sincerity|skill|skilled|skillful|skillfully|slammin|sleek|slick|smart|smarter|smartest|smartly|smile|smiles|smiling|smilingly|smitten|smooth|smoother|smoothes|smoothest|smoothly|snappy|snazzy|sociable|soft|softer|solace|solicitous|solicitously|solid|solidarity|soothe|soothingly|sophisticated|soulful|soundly|soundness|spacious|sparkle|sparkling|spectacular|spectacularly|speedily|speedy|spellbind|spellbinding|spellbindingly|spellbound|spirited|spiritual|splendid|splendidly|splendor|spontaneous|sporty|spotless|sprightly|stability|stabilize|stable|stainless|standout|state-of-the-art|stately|statuesque|staunch|staunchly|staunchness|steadfast|steadfastly|steadfastness|steadiest|steadiness|steady|stellar|stellarly|stimulate|stimulates|stimulating|stimulative|stirringly|straighten|straightforward|streamlined|striking|strikingly|striving|strong|stronger|strongest|stunned|stunning|stunningly|stupendous|stupendously|sturdier|sturdy|stylish|stylishly|stylized|suave|suavely|sublime|subsidize|subsidized|subsidizes|subsidizing|substantive|succeed|succeeded|succeeding|succeeds|succes|success|successes|successful|successfully|suffice|sufficed|suffices|sufficient|sufficiently|suitable|sumptuous|sumptuously|sumptuousness|super|superb|superbly|superior|superiority|supple|support|supported|supporter|supporting|supportive|supports|supremacy|supreme|supremely|supurb|supurbly|surmount|surpass|surreal|survival|survivor|sustainability|sustainable|swank|swankier|swankiest|swanky|sweeping|sweet|sweeten|sweetheart|sweetly|sweetness|swift|swiftness|talent|talented|talents|tantalize|tantalizing|tantalizingly|tempt|tempting|temptingly|tenacious|tenaciously|tenacity|tender|tenderly|terrific|terrifically|thank|thankful|thinner|thoughtful|thoughtfully|thoughtfulness|thrift|thrifty|thrill|thrilled|thrilling|thrillingly|thrills|thrive|thriving|thumb-up|thumbs-up|tickle|tidy|time-honored|timely|tingle|titillate|titillating|titillatingly|togetherness|tolerable|toll-free|top|top-notch|top-quality|topnotch|tops|tough|tougher|toughest|traction|tranquil|tranquility|transparent|treasure|tremendously|trendy|triumph|triumphal|triumphant|triumphantly|trivially|trophy|trouble-free|trump|trumpet|trust|trusted|trusting|trustingly|trustworthiness|trustworthy|trusty|truthful|truthfully|truthfulness|twinkly|ultra-crisp|unabashed|unabashedly|unaffected|unassailable|unbeatable|unbiased|unbound|uncomplicated|unconditional|undamaged|undaunted|understandable|undisputable|undisputably|undisputed|unencumbered|unequivocal|unequivocally|unfazed|unfettered|unforgettable|unity|unlimited|unmatched|unparalleled|unquestionable|unquestionably|unreal|unrestricted|unrivaled|unselfish|unwavering|upbeat|upgradable|upgradeable|upgraded|upheld|uphold|uplift|uplifting|upliftingly|upliftment|upscale|usable|useable|useful|user-friendly|user-replaceable|valiant|valiantly|valor|valuable|variety|venerate|verifiable|veritable|versatile|versatility|vibrant|vibrantly|victorious|victory|viewable|vigilance|vigilant|virtue|virtuous|virtuously|visionary|vivacious|vivid|vouch|vouchsafe|warm|warmer|warmhearted|warmly|warmth|wealthy|welcome|well|well-backlit|well-balanced|well-behaved|well-being|well-bred|well-connected|well-educated|well-established|well-informed|well-intentioned|well-known|well-made|well-managed|well-mannered|well-positioned|well-received|well-regarded|well-rounded|well-run|well-wishers|wellbeing|whoa|wholeheartedly|wholesome|whooa|whoooa|wieldy|willing|willingly|willingness|win|windfall|winnable|winner|winners|winning|wins|wisdom|wise|wisely|witty|won|wonder|wonderful|wonderfully|wonderous|wonderously|wonders|wondrous|woo|work|workable|worked|works|world-famous|worth|worth-while|worthiness|worthwhile|worthy|wow|wowed|wowing|wows|yay|youthful|zeal|zenith|zest|zippy)";
String neu = "(crazy|wierd)";
String neg ="(2-face|2-face|abnorma|abolis|abominabl|abominabl|abominat|abominatio|abor|aborte|abort|abrad|abrasiv|abrup|abruptl|abscon|absenc|absent-minde|absente|absur|absurdit|absurdl|absurdnes|abus|abuse|abuse|abusiv|abysma|abysmall|abys|accidenta|accos|accurse|accusatio|accusation|accus|accuse|accusin|accusingl|acerbat|acerbi|acerbicall|ach|ache|ache|ache|achin|acri|acridl|acridnes|acrimoniou|acrimoniousl|acrimon|adaman|adamantl|addic|addicte|addictin|addict|admonis|admonishe|admonishingl|admonishmen|admonitio|adulterat|adulterate|adulteratio|adulterie|adversaria|adversar|advers|adversit|afflic|afflictio|afflictiv|affron|afrai|aggravat|aggravatin|aggravatio|aggressio|aggressiv|aggressivenes|aggresso|aggriev|aggrieve|aggrivatio|aghas|agonie|agoniz|agonizin|agonizingl|agon|agroun|ai|ailin|ailmen|aimles|alar|alarme|alarmin|alarmingl|alienat|alienate|alienatio|allegatio|allegation|alleg|allergi|allergie|allerg|aloo|altercatio|ambiguit|ambiguou|ambivalenc|ambivalen|ambus|amis|amputat|anarchis|anarchis|anarchisti|anarch|anemi|ange|angril|angrines|angr|anguis|animosit|annihilat|annihilatio|anno|annoyanc|annoyance|annoye|annoyin|annoyingl|annoy|anomalou|anomal|antagonis|antagonis|antagonisti|antagoniz|anti|anti-america|anti-israel|anti-occupatio|anti-proliferatio|anti-semite|anti-socia|anti-u|anti-whit|antipath|antiquate|antithetica|anxietie|anxiet|anxiou|anxiousl|anxiousnes|apatheti|apatheticall|apath|apocalyps|apocalypti|apologis|apologist|appa|appal|appalle|appallin|appallingl|apprehensio|apprehension|apprehensiv|apprehensivel|arbitrar|arcan|archai|arduou|arduousl|argumentativ|arroganc|arrogan|arrogantl|ashame|asinin|asininel|asinininit|askanc|aspers|aspersio|aspersion|assai|assassi|assassinat|assaul|assul|astra|asunde|atrociou|atrocitie|atrocit|atroph|attac|attack|audaciou|audaciousl|audaciousnes|audacit|audiciousl|auster|authoritaria|autocra|autocrati|avalanch|avaric|avariciou|avariciousl|aveng|avers|aversio|awefu|awfu|awfull|awfulnes|awkwar|awkwardnes|a|babbl|back-logge|back-woo|back-wood|backach|backache|backachin|backbit|backbitin|backwar|backwardnes|backwoo|backwood|ba|badl|baffl|baffle|bafflemen|bafflin|bai|bal|bana|banaliz|ban|banis|banishmen|bankrup|barbaria|barbari|barbaricall|barbarit|barbarou|barbarousl|barre|baseles|bas|bashe|bashfu|bashin|bastar|bastard|battere|batterin|batt|bearis|beastl|bedla|bedlamit|befou|be|begga|beggarl|beggin|beguil|belabo|belate|beleague|beli|belittl|belittle|belittlin|bellicos|belligerenc|belligeren|belligerentl|bemoa|bemoanin|bemuse|ben|berat|bereav|bereavemen|beref|berser|beseec|bese|besieg|besmirc|bestia|betra|betraya|betrayal|betraye|betrayin|betray|bewai|bewar|bewilde|bewildere|bewilderin|bewilderingl|bewildermen|bewitc|bia|biase|biase|bicke|bickerin|bid-riggin|bigotrie|bigotr|bitc|bitch|bitin|bitingl|bitte|bitterl|bitternes|bizarr|bla|blabbe|blackmai|bla|blam|blameworth|blan|blandis|blasphem|blasphemou|blasphem|blaste|blatan|blatantl|blathe|blea|bleakl|bleaknes|blee|bleedin|bleed|blemis|blin|blindin|blindingl|blindsid|bliste|blisterin|bloate|blockag|blockhea|bloodshe|bloodthirst|blood|blotch|blo|blunde|blunderin|blunder|blun|blu|blurin|blurre|blurrin|blurr|blur|blur|boastfu|boggl|bogu|boi|boilin|boisterou|bom|bombar|bombardmen|bombasti|bondag|bonker|bor|bore|boredo|bore|borin|botc|bothe|bothere|botherin|bother|bothersom|bowdleriz|boycot|braggar|bragge|brainles|brainwas|bras|brashl|brashnes|bra|bravad|braze|brazenl|brazennes|breac|brea|break-u|break-up|breakdow|breakin|break|breaku|breakup|briber|brimston|bristl|brittl|brok|broke|broken-hearte|broo|browbea|bruis|bruise|bruise|bruisin|brusqu|bruta|brutalisin|brutalitie|brutalit|brutaliz|brutalizin|brutall|brut|brutis|b|buckl|bu|buggin|bugg|bug|bulkie|bulkines|bulk|bulkynes|bullshit|bull---|bullie|bullshi|bullshy|bull|bullyin|bullyingl|bu|bum|bumpe|bumpin|bumppin|bump|bump|bungl|bungle|bunglin|bun|burde|burdensom|burdensomel|bur|burne|burnin|burn|bus|bust|busybod|butche|butcher|buzzin|byzantin|cackl|calamitie|calamitou|calamitousl|calamit|callou|calumniat|calumniatio|calumnie|calumniou|calumniousl|calumn|cance|cancerou|canniba|cannibaliz|capitulat|capriciou|capriciousl|capriciousnes|capsiz|careles|carelessnes|caricatur|carnag|car|cartoonis|cash-strappe|castigat|castrate|casualt|cataclys|cataclysma|cataclysmi|cataclysmicall|catastroph|catastrophe|catastrophi|catastrophicall|catastrophie|causti|causticall|cautionar|cav|censur|chaf|chaf|chagri|challengin|chao|chaoti|chaste|chastis|chastisemen|chatte|chatterbo|chea|cheape|cheapl|chea|cheate|cheate|cheatin|cheat|checkere|cheerles|chees|chid|childis|chil|chill|chintz|chok|choleri|chopp|chor|chroni|chunk|clamo|clamorou|clas|clich|cliche|cliqu|clo|clogge|clog|clou|cloudin|cloud|clueles|clums|clunk|coars|cock|coerc|coercio|coerciv|col|coldl|collaps|collud|collusio|combativ|combus|comica|commiserat|commonplac|commotio|commotion|complacen|complai|complaine|complainin|complain|complain|complaint|comple|complicate|complicatio|complici|compulsio|compulsiv|conced|concede|concei|conceite|conce|concen|concer|concerne|concern|concessio|concession|condem|condemnabl|condemnatio|condemne|condemn|condescen|condescendin|condescendingl|condescensio|confes|confessio|confession|confine|conflic|conflicte|conflictin|conflict|confoun|confounde|confoundin|confron|confrontatio|confrontationa|confus|confuse|confuse|confusin|confusio|confusion|congeste|congestio|con|conscon|conservativ|conspicuou|conspicuousl|conspiracie|conspirac|conspirato|conspiratoria|conspir|consternatio|contagiou|contaminat|contaminate|contaminate|contaminatin|contaminatio|contemp|contemptibl|contemptuou|contemptuousl|conten|contentio|contentiou|contor|contortion|contradic|contradictio|contradictor|contrarines|contraven|contriv|contrive|controversia|controvers|convolute|corrod|corrosio|corrosion|corrosiv|corrup|corrupte|corruptin|corruptio|corrupt|corruptte|costlie|costl|counter-productiv|counterproductiv|coupist|covetou|cowar|cowardl|crabb|crac|cracke|crack|craftil|craftl|craft|cram|crampe|crampin|crank|cra|crapp|crap|cras|crashe|crashe|crashin|cras|crave|cravenl|craz|crazil|crazines|craz|crea|creakin|creak|credulou|cree|creepin|creep|creep|crep|crim|crimina|cring|cringe|cringe|crippl|cripple|cripple|cripplin|crisi|criti|critica|criticis|criticism|criticiz|criticize|criticizin|critic|cronyis|croo|crooke|crook|crowde|crowdednes|crud|crue|cruele|crueles|cruell|cruelnes|crueltie|cruelt|crumbl|crumblin|crumm|crumpl|crumple|crumple|crus|crushe|crushin|cr|culpabl|culpri|cumbersom|cun|cunt|cuplri|curs|curse|curse|cur|cus|cusse|cutthroa|cynica|cynicis|dm|damag|damage|damage|damagin|dam|damnabl|damnabl|damnatio|damne|damnin|dampe|dange|dangerou|dangerousnes|dar|darke|darkene|darke|darknes|dastar|dastardl|daun|dauntin|dauntingl|dawdl|daz|daze|dea|deadbea|deadloc|deadl|deadweigh|dea|deart|deat|debacl|debas|debasemen|debase|debatabl|debauc|debauche|debaucher|debilitat|debilitatin|debilit|deb|debt|decadenc|decaden|deca|decaye|decei|deceitfu|deceitfull|deceitfulnes|deceiv|deceive|deceiver|deceivin|deceptio|deceptiv|deceptivel|declai|declin|decline|declinin|decremen|decrepi|decrepitud|decr|defamatio|defamation|defamator|defam|defec|defectiv|defect|defensiv|defianc|defian|defiantl|deficiencie|deficienc|deficien|defil|defile|defor|deforme|defraudin|defunc|def|degenerat|degeneratel|degeneratio|degradatio|degrad|degradin|degradingl|dehumanizatio|dehumaniz|deig|dejec|dejecte|dejectedl|dejectio|dela|delaye|delayin|delay|delinquenc|delinquen|deliriou|deliriu|delud|delude|delug|delusio|delusiona|delusion|demea|demeanin|demis|demolis|demolishe|demo|demoni|demoniz|demonize|demonize|demonizin|demoraliz|demoralizin|demoralizingl|denia|denie|denie|denigrat|denounc|dens|den|dente|dent|denunciat|denunciatio|denunciation|den|denyin|deplet|deplorabl|deplorabl|deplor|deplorin|deploringl|deprav|deprave|depravedl|deprecat|depres|depresse|depressin|depressingl|depressio|depression|depriv|deprive|derid|derisio|derisiv|derisivel|derisivenes|derogator|desecrat|deser|desertio|desiccat|desiccate|desititut|desolat|desolatel|desolatio|despai|despairin|despairingl|desperat|desperatel|desperatio|despicabl|despicabl|despis|despise|despoi|despoile|despondenc|despondenc|desponden|despondentl|despo|despoti|despotis|destabilisatio|destain|destitut|destitutio|destro|destroye|destructio|destructiv|desultor|dete|deteriorat|deterioratin|deterioratio|deterren|detes|detestabl|detestabl|deteste|detestin|detest|detrac|detracte|detractin|detractio|detract|detrimen|detrimenta|devastat|devastate|devastate|devastatin|devastatingl|devastatio|deviat|deviatio|devi|devilis|devilishl|devilmen|devilr|deviou|deviousl|deviousnes|devoi|diaboli|diabolica|diabolicall|diametricall|diappointe|diatrib|diatribe|dic|dictato|dictatoria|di|die-har|die|die|difficul|difficultie|difficult|diffidenc|dilapidate|dilemm|dilly-dall|di|dimme|di|din|ding|dink|dir|direl|direnes|dir|dirtba|dirtbag|dirt|dirt|disabl|disable|disaccor|disadvantag|disadvantage|disadvantageou|disadvantage|disaffec|disaffecte|disaffir|disagre|disagreeabl|disagreeabl|disagree|disagreein|disagreemen|disagree|disallo|disapointe|disapointin|disapointmen|disappoin|disappointe|disappointin|disappointingl|disappointmen|disappointment|disappoint|disapprobatio|disapprova|disapprov|disapprovin|disar|disarra|disaste|disasterou|disastrou|disastrousl|disavo|disavowa|disbelie|disbeliev|disbelieve|disclai|discombobulat|discomfi|discomfititur|discomfor|discompos|disconcer|disconcerte|disconcertin|disconcertingl|disconsolat|disconsolatel|disconsolatio|disconten|discontente|discontentedl|discontinue|discontinuit|discontinuou|discor|discordanc|discordan|discountenanc|discourag|discouragemen|discouragin|discouragingl|discourteou|discourteousl|discoutinou|discredi|discrepan|discriminat|discriminatio|discriminator|disdai|disdaine|disdainfu|disdainfull|disfavo|disgrac|disgrace|disgracefu|disgracefull|disgruntl|disgruntle|disgus|disguste|disgustedl|disgustfu|disgustfull|disgustin|disgustingl|dishearte|disheartenin|dishearteningl|dishones|dishonestl|dishonest|dishono|dishonorabl|dishonorablel|disillusio|disillusione|disillusionmen|disillusion|disinclinatio|disincline|disingenuou|disingenuousl|disintegrat|disintegrate|disintegrate|disintegratio|disinteres|disintereste|dislik|dislike|dislike|dislikin|dislocate|disloya|disloyalt|disma|dismall|dismalnes|disma|dismaye|dismayin|dismayingl|dismissiv|dismissivel|disobedienc|disobedien|disobe|disoobedien|disorde|disordere|disorderl|disorganize|disorien|disoriente|disow|disparag|disparagin|disparagingl|dispensabl|dispiri|dispirite|dispiritedl|dispiritin|displac|displace|displeas|displease|displeasin|displeasur|disproportionat|disprov|disputabl|disput|dispute|disquie|disquietin|disquietingl|disquietud|disregar|disregardfu|disreputabl|disreput|disrespec|disrespectabl|disrespectablit|disrespectfu|disrespectfull|disrespectfulnes|disrespectin|disrup|disruptio|disruptiv|dis|dissapointe|dissappointe|dissappointin|dissatisfactio|dissatisfactor|dissatisfie|dissatisfie|dissatisf|dissatisfyin|disse|dissembl|dissemble|dissensio|dissen|dissente|dissentio|disservic|disse|dissidenc|dissiden|dissident|dissin|dissocia|dissolut|dissolutio|dissonanc|dissonan|dissonantl|dissuad|dissuasiv|distain|distast|distastefu|distastefull|distor|distorte|distortio|distort|distrac|distractin|distractio|distraugh|distraughtl|distraughtnes|distres|distresse|distressin|distressingl|distrus|distrustfu|distrustin|distur|disturbanc|disturbe|disturbin|disturbingl|disunit|disvalu|divergen|divisiv|divisivel|divisivenes|dizzin|dizzingl|dizz|dodderin|dodge|dogge|doggedl|dogmati|doldrum|dominee|domineerin|donsid|doo|doome|doomsda|dop|doub|doubtfu|doubtfull|doubt|douchba|doucheba|douchebag|downbea|downcas|downe|downfal|downfalle|downgrad|downhearte|downheartedl|downhil|downsid|downside|downtur|downturn|dra|draconia|draconi|dra|dragge|draggin|dragoo|drag|drai|draine|drainin|drain|drasti|drasticall|drawbac|drawback|drea|dreadfu|dreadfull|dreadfulnes|drear|drippe|drippin|dripp|drip|drone|droo|droop|drop-ou|drop-out|dropou|dropout|drough|drownin|drun|drunkar|drunke|dubiou|dubiousl|dubitabl|du|dul|dullar|dum|dumbfoun|dum|dumpe|dumpin|dump|dunc|dungeo|dungeon|dup|dus|dust|dwindlin|dyin|earsplittin|eccentri|eccentricit|effig|effronter|egocentri|egomani|egotis|egotistica|egotisticall|egregiou|egregiousl|election-rigge|eliminatio|emaciate|emasculat|embarras|embarrassin|embarrassingl|embarrassmen|embattle|embroi|embroile|embroilmen|emergenc|emphati|emphaticall|emptines|encroac|encroachmen|endange|enemie|enem|enervat|enfeebl|enflam|engul|enjoi|enmit|enrag|enrage|enragin|enslav|entangl|entanglemen|entra|entrapmen|enviou|enviousl|enviousnes|epidemi|equivoca|eras|erod|erode|erosio|er|erran|errati|erraticall|erroneou|erroneousl|erro|error|eruption|escapad|esche|estrange|evad|evasio|evasiv|evi|evildoe|evil|eviscerat|exacerbat|exagerat|exagerate|exagerate|exaggerat|exaggeratio|exasperat|exasperate|exasperatin|exasperatingl|exasperatio|excessiv|excessivel|exclusio|excoriat|excruciatin|excruciatingl|excus|excuse|execrat|exhaus|exhauste|exhaustio|exhaust|exhorbitan|exhor|exil|exorbitan|exorbitantanc|exorbitantl|expe|expensiv|expir|expire|explod|exploi|exploitatio|explosiv|expropriat|expropriatio|expuls|expung|exterminat|exterminatio|extinguis|extor|extortio|extraneou|extravaganc|extravagan|extravagantl|extremis|extremis|extremist|eyesor|f|fabricat|fabricatio|facetiou|facetiousl|fai|faile|failin|fail|failur|failure|fain|fainthearte|faithles|fak|fal|fallacie|fallaciou|fallaciousl|fallaciousnes|fallac|falle|fallin|fallou|fall|FALS|falsehoo|falsel|falsif|falte|faltere|famin|famishe|fanati|fanatica|fanaticall|fanaticis|fanatic|fancifu|far-fetche|farc|farcica|farcical-yet-provocativ|farcicall|farfetche|fascis|fascis|fastidiou|fastidiousl|fastuou|fa|fat-ca|fat-cat|fata|fatalisti|fatalisticall|fatall|fatca|fatcat|fatefu|fatefull|fathomles|fatigu|fatigue|fatiqu|fatt|fatuit|fatuou|fatuousl|faul|fault|fault|fawningl|faz|fea|fearfu|fearfull|fear|fearsom|feckles|feebl|feeblel|feebleminde|feig|fein|fel|felo|feloniou|ferociousl|ferocit|feti|feve|feveris|fever|fiasc|fi|fibbe|fickl|fictio|fictiona|fictitiou|fidge|fidget|fien|fiendis|fierc|figurehea|filt|filth|finagl|finick|fissure|fis|flabbergas|flabbergaste|flaggin|flagran|flagrantl|flai|flair|fla|flak|flake|flakienes|flakin|flak|flar|flare|flareu|flareup|flat-ou|flaun|fla|flawe|flaw|fle|flee|fleein|flee|flee|fleetin|flicerin|flicke|flickerin|flicker|flight|flimfla|flims|flir|flirt|floore|flounde|flounderin|flou|fluste|fo|foo|foole|foolhard|foolis|foolishl|foolishnes|forbi|forbidde|forbiddin|forcefu|forebodin|forebodingl|forfei|forge|forgetfu|forgetfull|forgetfulnes|forlor|forlornl|forsak|forsake|forswea|fou|foull|foulnes|fractiou|fractiousl|fractur|fragil|fragmente|frai|franti|franticall|franticl|frau|fraudulen|fraugh|frazzl|frazzle|frea|freakin|freakis|freakishl|freak|freez|freeze|freezin|freneti|freneticall|frenzie|frenz|fre|fretfu|fret|frictio|friction|frie|friggi|friggin|frigh|frighte|frightenin|frighteningl|frightfu|frightfull|frigi|fros|frow|froz|froze|fruitles|fruitlessl|frustrat|frustrate|frustrate|frustratin|frustratingl|frustratio|frustration|fuc|fuckin|fudg|fugitiv|full-blow|fulminat|fumbl|fum|fume|fundamentalis|funk|funnil|funn|furiou|furiousl|furo|fur|fus|fuss|fustigat|fust|futil|futilel|futilit|fuzz|gabbl|gaf|gaff|gainsa|gainsaye|gal|gallin|gallingl|gall|gangste|gap|garbag|garis|gas|gauch|gaud|gaw|gawk|geeze|genocid|get-ric|ghastl|ghett|ghostin|gibbe|gibberis|gib|gidd|gimmic|gimmicke|gimmickin|gimmick|gimmick|glar|glaringl|gli|glibl|glitc|glitche|gloatingl|gloo|gloom|glowe|glu|glu|gnawin|goa|goadin|god-awfu|goo|goof|goo|gossi|graceles|gracelessl|graf|grain|grappl|grat|gratin|gravel|greas|gree|greed|grie|grievanc|grievance|griev|grievin|grievou|grievousl|gri|grimac|grin|grip|gripe|grisl|gritt|gros|grossl|grotesqu|grouc|grouch|groundles|grous|grow|grudg|grudge|grudgin|grudgingl|gruesom|gruesomel|gruf|grumbl|grumpie|grumpies|grumpil|grumpis|grump|guil|guil|guiltil|guilt|gullibl|gutles|gutte|hac|hack|haggar|haggl|hairlos|halfhearte|halfheartedl|hallucinat|hallucinatio|hampe|hampere|handicappe|han|hang|haphazar|haples|harangu|haras|harasse|harasse|harassmen|harborin|harbor|har|hard-hi|hard-lin|hard-line|hardbal|harde|hardene|hardheade|hardhearte|hardline|hardliner|hardshi|hardship|har|harme|harmfu|harm|harp|harrida|harrie|harro|hars|harshl|hasselin|hassl|hassle|hassle|hast|hastil|hast|hat|hate|hatefu|hatefull|hatefulnes|hate|hater|hate|hatin|hatre|haughtil|haught|haun|hauntin|havo|hawkis|haywir|hazar|hazardou|haz|haz|head-ache|headach|headache|heartbreake|heartbreakin|heartbreakingl|heartles|heathe|heavy-hande|heavyhearte|hec|heckl|heckle|heckle|hecti|hedg|hedonisti|heedles|heft|hegemonis|hegemonisti|hegemon|heinou|hel|hell-ben|hellio|hell|helples|helplessl|helplessnes|heres|hereti|heretica|hesitan|hestitan|hideou|hideousl|hideousnes|high-price|hiliariou|hinde|hindranc|his|hisse|hissin|ho-hu|hoar|hoa|hobbl|hog|hollo|hoodiu|hoodwin|hooliga|hopeles|hopelessl|hopelessnes|hord|horrendou|horrendousl|horribl|horri|horrifi|horrifie|horrifie|horrif|horrifyin|horrify|hostag|hostil|hostilitie|hostilit|hotbed|hothea|hotheade|hothous|hubri|huckste|hu|humi|humiliat|humiliatin|humiliatio|hummin|hun|hur|hurte|hurtfu|hurtin|hurt|hustle|hyp|hypocric|hypocris|hypocrit|hypocrite|hypocritica|hypocriticall|hysteri|hysteri|hysterica|hystericall|hysteric|idiocie|idioc|idio|idioti|idioticall|idiot|idl|ignobl|ignominiou|ignominiousl|ignomin|ignoranc|ignoran|ignor|ill-advise|ill-conceive|ill-define|ill-designe|ill-fate|ill-favore|ill-forme|ill-mannere|ill-nature|ill-sorte|ill-tempere|ill-treate|ill-treatmen|ill-usag|ill-use|illega|illegall|illegitimat|illici|illiterat|illnes|illogi|illogica|illogicall|illusio|illusion|illusor|imaginar|imbalanc|imbecil|imbrogli|immateria|immatur|imminenc|imminentl|immobilize|immoderat|immoderatel|immodes|immora|immoralit|immorall|immovabl|impai|impaire|impass|impatienc|impatien|impatientl|impeac|impedanc|imped|impedimen|impendin|impeniten|imperfec|imperfectio|imperfection|imperfectl|imperialis|imperi|imperiou|imperiousl|impermissibl|impersona|impertinen|impetuou|impetuousl|impiet|imping|impiou|implacabl|implausibl|implausibl|implicat|implicatio|implod|impolit|impolitel|impoliti|importunat|importun|impos|imposer|imposin|impositio|impossibl|impossiblit|impossibl|impoten|impoveris|impoverishe|impractica|imprecat|imprecis|imprecisel|imprecisio|impriso|imprisonmen|improbabilit|improbabl|improbabl|imprope|improperl|impropriet|imprudenc|impruden|impudenc|impuden|impudentl|impug|impulsiv|impulsivel|impunit|impur|impurit|inabilit|inaccuracie|inaccurac|inaccurat|inaccuratel|inactio|inactiv|inadequac|inadequat|inadequatel|inadveren|inadverentl|inadvisabl|inadvisabl|inan|inanel|inappropriat|inappropriatel|inap|inaptitud|inarticulat|inattentiv|inaudibl|incapabl|incapabl|incautiou|incendiar|incens|incessan|incessantl|incit|incitemen|incivilit|inclemen|incognizan|incoherenc|incoheren|incoherentl|incommensurat|incomparabl|incomparabl|incompatabilit|incompatibilit|incompatibl|incompetenc|incompeten|incompetentl|incomplet|incomplian|incomprehensibl|incomprehensio|inconceivabl|inconceivabl|incongruou|incongruousl|inconsequen|inconsequentia|inconsequentiall|inconsequentl|inconsiderat|inconsideratel|inconsistenc|inconsistencie|inconsistenc|inconsisten|inconsolabl|inconsolabl|inconstan|inconvenienc|inconvenientl|incorrec|incorrectl|incorrigibl|incorrigibl|incredulou|incredulousl|inculcat|indecenc|indecen|indecentl|indecisio|indecisiv|indecisivel|indecoru|indefensibl|indelicat|indeterminabl|indeterminabl|indeterminat|indifferenc|indifferen|indigen|indignan|indignantl|indignatio|indignit|indiscernibl|indiscree|indiscreetl|indiscretio|indiscriminat|indiscriminatel|indiscriminatin|indistinguishabl|indoctrinat|indoctrinatio|indolen|indulg|ineffectiv|ineffectivel|ineffectivenes|ineffectua|ineffectuall|ineffectualnes|inefficaciou|inefficac|inefficienc|inefficien|inefficientl|ineleganc|inelegan|ineligibl|ineloquen|ineloquentl|inep|ineptitud|ineptl|inequalitie|inequalit|inequitabl|inequitabl|inequitie|inescapabl|inescapabl|inessentia|inevitabl|inevitabl|inexcusabl|inexcusabl|inexorabl|inexorabl|inexperienc|inexperience|inexper|inexpertl|inexpiabl|inexplainabl|inextricabl|inextricabl|infamou|infamousl|infam|infecte|infectio|infection|inferio|inferiorit|inferna|infes|infeste|infide|infidel|infiltrato|infiltrator|infir|inflam|inflammatio|inflammator|inflamme|inflate|inflationar|inflexibl|inflic|infractio|infring|infringemen|infringement|infuriat|infuriate|infuriatin|infuriatingl|ingloriou|ingrat|ingratitud|inhibi|inhibitio|inhospitabl|inhospitalit|inhuma|inhuman|inhumanit|inimica|inimicall|iniquitou|iniquit|injudiciou|injur|injuriou|injur|injustic|injustice|innuend|inoperabl|inopportun|inordinat|inordinatel|insan|insanel|insanit|insatiabl|insecur|insecurit|insensibl|insensitiv|insensitivel|insensitivit|insidiou|insidiousl|insignificanc|insignifican|insignificantl|insincer|insincerel|insincerit|insinuat|insinuatin|insinuatio|insociabl|insolenc|insolen|insolentl|insolven|insoucianc|instabilit|instabl|instigat|instigato|instigator|insubordinat|insubstantia|insubstantiall|insufferabl|insufferabl|insufficienc|insufficien|insufficientl|insula|insul|insulte|insultin|insultingl|insult|insupportabl|insupportabl|insurmountabl|insurmountabl|insurrectio|intefer|intefere|intens|interfer|interferenc|interfere|intermitten|interrup|interruptio|interruption|intimidat|intimidatin|intimidatingl|intimidatio|intolerabl|intolerablel|intoleranc|intoxicat|intractabl|intransigenc|intransigen|intrud|intrusio|intrusiv|inundat|inundate|invade|invali|invalidat|invalidit|invasiv|invectiv|inveigl|invidiou|invidiousl|invidiousnes|invisibl|involuntaril|involuntar|irascibl|irat|iratel|ir|ir|irke|irkin|irk|irksom|irksomel|irksomenes|irksomenesse|ironi|ironica|ironicall|ironie|iron|irragularit|irrationa|irrationalitie|irrationalit|irrationall|irrational|irreconcilabl|irrecoverabl|irrecoverablenes|irrecoverablenesse|irrecoverabl|irredeemabl|irredeemabl|irreformabl|irregula|irregularit|irrelevanc|irrelevan|irreparabl|irreplacibl|irrepressibl|irresolut|irresolvabl|irresponsibl|irresponsibl|irretatin|irretrievabl|irreversibl|irritabl|irritabl|irritan|irritat|irritate|irritatin|irritatio|irritation|isolat|isolate|isolatio|issu|issue|itc|itchin|itch|jabbe|jade|jagge|ja|jarrin|jaundice|jealou|jealousl|jealousnes|jealous|jee|jeerin|jeeringl|jeer|jeopardiz|jeopard|jer|jerk|jitte|jitter|jitter|job-killin|jobles|jok|joke|jol|judde|judderin|judder|jump|jun|junk|junkyar|jutte|jutter|kapu|kil|kille|kille|killin|killjo|kill|knav|knif|knoc|knotte|koo|kook|lac|lackadaisica|lacke|lacke|lackey|lackin|lackluste|lack|laconi|la|lagge|laggin|lagg|lag|laid-of|lambas|lambast|lam|lame-duc|lamen|lamentabl|lamentabl|langui|languis|languo|languorou|languorousl|lank|laps|lapse|lapse|lasciviou|last-ditc|latenc|laughabl|laughabl|laughingstoc|lawbreake|lawbreakin|lawles|lawlessnes|layof|layoff-happ|laz|lea|leakag|leakage|leakin|leak|leak|lec|leche|lecherou|lecher|leec|lee|leer|left-leanin|lemo|length|less-develope|lesser-know|letc|letha|lethargi|letharg|lew|lewdl|lewdnes|liabilit|liabl|lia|liar|licentiou|licentiousl|licentiousnes|li|lie|lie|lie|life-threatenin|lifeles|limi|limitatio|limitation|limite|limit|lim|listles|litigiou|little-know|livi|lividl|loat|loath|loathin|loathl|loathsom|loathsomel|lon|lonelines|lonel|lone|lonesom|long-tim|long-winde|longin|longingl|loophol|loophole|loos|loo|lor|los|lose|loser|lose|losin|los|losse|los|lou|loude|lous|loveles|lovelor|low-rate|lowl|ludicrou|ludicrousl|lugubriou|lukewar|lul|lump|lunati|lunaticis|lurc|lur|luri|lur|lurkin|lyin|macabr|ma|madde|maddenin|maddeningl|madde|madl|madma|madnes|maladjuste|maladjustmen|malad|malais|malconten|malcontente|maledic|malevolenc|malevolen|malevolentl|malic|maliciou|maliciousl|maliciousnes|malig|malignan|malodorou|maltreatmen|mangl|mangle|mangle|manglin|mani|mania|maniaca|mani|manipulat|manipulatio|manipulativ|manipulator|ma|margina|marginall|martyrdo|martyrdom-seekin|mashe|massacr|massacre|matt|mawkis|mawkishl|mawkishnes|meage|meaningles|meannes|measl|meddl|meddlesom|mediocr|mediocrit|melanchol|melodramati|melodramaticall|meltdow|menac|menacin|menacingl|mendaciou|mendacit|menia|merciles|mercilessl|mes|messe|messe|messin|mess|midge|mif|militanc|mindles|mindlessl|mirag|mir|misalig|misaligne|misalign|misapprehen|misbecom|misbecomin|misbegotte|misbehav|misbehavio|miscalculat|miscalculatio|miscellaneou|mischie|mischievou|mischievousl|misconceptio|misconception|miscrean|miscreant|misdirectio|mise|miserabl|miserablenes|miserabl|miserie|miserl|miser|misfi|misfortun|misgivin|misgiving|misguidanc|misguid|misguide|mishandl|misha|misinfor|misinforme|misinterpre|misjudg|misjudgmen|mislea|misleadin|misleadingl|mislik|mismanag|mispronounc|mispronounce|mispronounce|misrea|misreadin|misrepresen|misrepresentatio|mis|misse|misse|misstatemen|mis|mistak|mistake|mistakenl|mistake|mistifie|mistres|mistrus|mistrustfu|mistrustfull|mist|misunderstan|misunderstandin|misunderstanding|misunderstoo|misus|moa|mobste|moc|mocke|mockerie|mocker|mockin|mockingl|mock|moles|molestatio|monotonou|monoton|monste|monstrositie|monstrosit|monstrou|monstrousl|mood|moo|mop|morbi|morbidl|mordan|mordantl|moribun|moro|moroni|moron|mortificatio|mortifie|mortif|mortifyin|motionles|motle|mour|mourne|mournfu|mournfull|muddl|mudd|mudslinge|mudslingin|mulis|multi-polarizatio|mundan|murde|murdere|murderou|murderousl|murk|muscle-flexin|mush|must|mysteriou|mysteriousl|myster|mystif|myt|na|naggin|naiv|naivel|narrowe|nastil|nastines|nast|naught|nauseat|nauseate|nauseatin|nauseatingl|na\u00cc\u00d3v|nebulou|nebulousl|needles|needlessl|need|nefariou|nefariousl|negat|negatio|negativ|negative|negativit|neglec|neglecte|negligenc|negligen|nemesi|nepotis|nervou|nervousl|nervousnes|nettl|nettlesom|neuroti|neuroticall|niggl|niggle|nightmar|nightmaris|nightmarishl|nitpic|nitpickin|nois|noise|noisie|nois|non-confidenc|nonexisten|nonresponsiv|nonsens|nose|notoriet|notoriou|notoriousl|noxiou|nuisanc|num|obes|objec|objectio|objectionabl|objection|obliqu|obliterat|obliterate|obliviou|obnoxiou|obnoxiousl|obscen|obscenel|obscenit|obscur|obscure|obscure|obscurit|obses|obsessiv|obsessivel|obsessivenes|obsolet|obstacl|obstinat|obstinatel|obstruc|obstructe|obstructin|obstructio|obstruct|obtrusiv|obtus|occlud|occlude|occlude|occludin|od|odde|oddes|odditie|oddit|oddl|odo|offenc|offen|offende|offendin|offense|offensiv|offensivel|offensivenes|officiou|ominou|ominousl|omissio|omi|one-side|onerou|onerousl|onslaugh|opinionate|opponen|opportunisti|oppos|oppositio|opposition|oppres|oppressio|oppressiv|oppressivel|oppressivenes|oppressor|ordea|orpha|ostraciz|outbrea|outburs|outburst|outcas|outcr|outla|outmode|outrag|outrage|outrageou|outrageousl|outrageousnes|outrage|outside|over-acte|over-aw|over-balance|over-hype|over-price|over-valuatio|overac|overacte|overaw|overbalanc|overbalance|overbearin|overbearingl|overblow|overd|overdon|overdu|overemphasiz|overhea|overkil|overloade|overloo|overpai|overpaye|overpla|overpowe|overprice|overrate|overreac|overru|overshado|oversigh|oversight|oversimplificatio|oversimplifie|oversimplif|oversiz|overstat|overstate|overstatemen|overstatement|overstate|overtaxe|overthro|overthrow|overtur|overweigh|overwhel|overwhelme|overwhelmin|overwhelmingl|overwhelm|overzealou|overzealousl|overzelou|pai|painfu|painful|painfull|pain|pal|pale|paltr|pa|pandemoniu|pande|panderin|pander|pani|panic|panicke|panickin|panick|paradoxica|paradoxicall|paraliz|paralyze|paranoi|paranoi|parasit|paria|parod|partialit|partisa|partisan|pass|passiv|passivenes|patheti|patheticall|patroniz|paucit|paupe|pauper|paybac|peculia|peculiarl|pedanti|peele|peev|peeve|peevis|peevishl|penaliz|penalt|perfidiou|perfidit|perfunctor|peri|perilou|perilousl|peris|perniciou|perple|perplexe|perplexin|perplexit|persecut|persecutio|pertinaciou|pertinaciousl|pertinacit|pertur|perturbe|pervasiv|pervers|perversel|perversio|perversit|perver|perverte|pervert|pessimis|pessimisti|pessimisticall|pes|pestilen|petrifie|petrif|pettifo|pett|phobi|phobi|phon|picke|pickete|picketin|picket|pick|pi|pig|pillag|pillor|pimpl|pinc|piqu|pitiabl|pitifu|pitifull|pitiles|pitilessl|pittanc|pit|plagiariz|plagu|plastick|playthin|ple|plea|plebeia|pligh|plo|plotter|plo|plunde|plundere|pointles|pointlessl|poiso|poisonou|poisonousl|poke|pok|polarisatio|polemiz|pollut|pollute|polluter|polutio|pompou|poo|poore|poores|poorl|posturin|pou|povert|powerles|prat|pratfal|prattl|precariou|precariousl|precipitat|precipitou|predator|predicamen|prejudg|prejudic|prejudice|prejudicia|premeditate|preoccup|preposterou|preposterousl|presumptuou|presumptuousl|pretenc|preten|pretens|pretentiou|pretentiousl|prevaricat|price|pricie|pric|prickl|prickle|pridefu|pri|primitiv|priso|prisone|proble|problemati|problem|procrastinat|procrastinate|procrastinatio|profan|profanit|prohibi|prohibitiv|prohibitivel|propagand|propagandiz|proprietar|prosecut|protes|proteste|protestin|protest|protracte|provocatio|provocativ|provok|pr|pugnaciou|pugnaciousl|pugnacit|punc|punis|punishabl|punitiv|pun|pun|puppe|puppet|puzzle|puzzlemen|puzzlin|quac|qual|qualm|quandar|quarre|quarrellou|quarrellousl|quarrel|quarrelsom|quas|quee|questionabl|quibbl|quibble|quitte|rabi|racis|racis|racist|rac|radica|radicalizatio|radicall|radical|rag|ragge|ragin|rai|rake|rampag|rampan|ramshackl|ranco|randoml|rankl|ran|rante|rantin|rantingl|rant|rap|rape|rapin|rasca|rascal|ras|rattl|rattle|rattle|ravag|ravin|reactionar|rebelliou|rebuf|rebuk|recalcitran|recan|recessio|recessionar|reckles|recklessl|recklessnes|recoi|recourse|redundanc|redundan|refusa|refus|refuse|refuse|refusin|refutatio|refut|refute|refute|refutin|regres|regressio|regressiv|regre|regrete|regretfu|regretfull|regret|regrettabl|regrettabl|regrette|rejec|rejecte|rejectin|rejectio|reject|relaps|relentles|relentlessl|relentlessnes|reluctanc|reluctan|reluctantl|remors|remorsefu|remorsefull|remorseles|remorselessl|remorselessnes|renounc|renunciatio|repe|repetitiv|reprehensibl|reprehensibl|reprehensio|reprehensiv|repres|repressio|repressiv|repriman|reproac|reproachfu|reprov|reprovingl|repudiat|repudiatio|repug|repugnanc|repugnan|repugnantl|repuls|repulse|repulsin|repulsiv|repulsivel|repulsivenes|resen|resentfu|resentmen|resignatio|resigne|resistanc|restles|restlessnes|restric|restricte|restrictio|restrictiv|resurgen|retaliat|retaliator|retar|retarde|retardednes|retard|reticen|retrac|retrea|retreate|reveng|revengefu|revengefull|rever|revil|revile|revok|revol|revoltin|revoltingl|revulsio|revulsiv|rhapsodiz|rhetori|rhetorica|rice|ridicul|ridicule|ridiculou|ridiculousl|rif|rif|rift|rigi|rigidit|rigidnes|ril|rile|ri|rip-of|ripof|rippe|ris|risk|risk|riva|rivalr|roadblock|rock|rogu|rollercoaste|ro|rotte|roug|rremediabl|rubbis|rud|ru|ruffia|ruffl|rui|ruine|ruinin|ruinou|ruin|rumblin|rumo|rumor|rumour|rumpl|run-dow|runawa|ruptur|rus|rust|rust|ru|ruthles|ruthlessl|ruthlessnes|rut|sabotag|sac|sacrifice|sa|sadde|sadl|sadnes|sa|sagge|saggin|sagg|sag|salaciou|sanctimoniou|sa|sarcas|sarcasti|sarcasticall|sardoni|sardonicall|sas|satirica|satiriz|savag|savage|savager|savage|scal|sca|scam|scanda|scandaliz|scandalize|scandalou|scandalousl|scandal|scande|scandel|scan|scapegoa|sca|scarc|scarcel|scarcit|scar|scare|scarie|scaries|scaril|scarre|scar|scar|scathin|scathingl|sceptica|scof|scoffingl|scol|scolde|scoldin|scoldingl|scorchin|scorchingl|scor|scornfu|scornfull|scoundre|scourg|scow|scrambl|scramble|scramble|scramblin|scra|scratc|scratche|scratche|scratch|screa|screec|screw-u|screwe|screwed-u|screw|scuf|scuff|scu|scumm|second-clas|second-tie|secretiv|sedentar|seed|seeth|seethin|self-cou|self-criticis|self-defeatin|self-destructiv|self-humiliatio|self-interes|self-intereste|self-servin|selfintereste|selfis|selfishl|selfishnes|semi-retarde|senil|sensationaliz|senseles|senselessl|seriousnes|sermoniz|servitud|set-u|setbac|setback|seve|sever|severit|sh|shabb|shadow|shad|shak|shak|shallo|sha|shamble|sham|shamefu|shamefull|shamefulnes|shameles|shamelessl|shamelessnes|shar|sharpl|shatte|shemal|shimme|shimm|shipwrec|shir|shirke|shi|shive|shoc|shocke|shockin|shockingl|shodd|short-live|shortag|shortchang|shortcomin|shortcoming|shortnes|shortsighte|shortsightednes|showdow|shre|shrie|shril|shrill|shrive|shrou|shroude|shru|shu|shunne|sic|sicke|sickenin|sickeningl|sickl|sicknes|sidetrac|sidetracke|sieg|sillil|sill|simplisti|simplisticall|si|sinfu|sinfull|siniste|sinisterl|sin|sinkin|skeleton|skepti|skeptica|skepticall|skepticis|sketch|skimp|skinn|skittis|skittishl|skul|slac|slande|slandere|slanderou|slanderousl|slander|sla|slashin|slaughte|slaughtere|slav|slave|sleaz|slim|slo|slogge|sloggin|slog|sloooooooooooooo|sloooo|slooo|sloo|sloppil|slopp|slot|slothfu|slo|slow-movin|slowe|slowe|slowes|slowl|slow|sloww|slowww|slu|sluggis|slum|slumpin|slumppin|slu|slu|slut|sl|smac|smallis|smas|smea|smel|smelle|smellin|smell|smell|smel|smok|smokescree|smolde|smolderin|smothe|smoulde|smoulderin|smudg|smudge|smudge|smudgin|smu|smugl|smu|smuttie|smutties|smutt|sna|snagge|snaggin|snag|snappis|snappishl|snar|snark|snar|snea|sneakil|sneak|snee|sneerin|sneeringl|sno|snobbis|snobb|snobis|snob|snu|so-ca|soap|so|sobe|soberin|solem|solicitud|sombe|sor|sorel|sorenes|sorro|sorrowfu|sorrowfull|sorr|sou|sourl|spad|span|spend|spe|spewe|spewin|spew|spillin|spinste|spiritles|spit|spitefu|spitefull|spitefulnes|splatte|spli|splittin|spoi|spoilag|spoilage|spoile|spoille|spoil|spoo|spookie|spookies|spookil|spook|spoon-fe|spoon-fee|spoonfe|sporadi|spott|spuriou|spur|sputte|squabbl|squabblin|squande|squas|squea|squeak|squeak|squea|squealin|squeal|squir|sta|stagnan|stagnat|stagnatio|stai|stai|stain|stal|stalemat|stal|stall|stamme|stamped|standstil|star|starkl|startl|startlin|startlingl|starvatio|starv|stati|stea|stealin|steal|stee|steepl|stenc|stereotyp|stereotypica|stereotypicall|ster|ste|stick|stif|stiffnes|stifl|stiflin|stiflingl|stigm|stigmatiz|stin|stingin|stingingl|sting|stin|stink|stodg|stol|stole|stoog|stooge|storm|straggl|straggle|strai|straine|strainin|strang|strangel|strange|stranges|strangl|streak|strenuou|stres|stresse|stressfu|stressfull|stricke|stric|strictl|striden|stridentl|strif|strik|stringen|stringentl|struc|struggl|struggle|struggle|strugglin|stru|stubbor|stubbornl|stubbornnes|stuc|stuff|stumbl|stumble|stumble|stum|stumpe|stump|stu|stun|stunte|stupi|stupides|stupidit|stupidl|stupifie|stupif|stupo|stutte|stuttere|stutterin|stutter|st|stymie|sub-pa|subdue|subjecte|subjectio|subjugat|subjugatio|submissiv|subordinat|subpoen|subpoena|subservienc|subservien|substandar|subtrac|subversio|subversiv|subversivel|subver|succum|suc|sucke|sucke|suck|suck|su|sue|suein|sue|suffe|suffere|suffere|sufferer|sufferin|suffer|suffocat|sugar-coa|sugar-coate|sugarcoate|suicida|suicid|sul|sulle|sull|sunde|sun|sunke|superficia|superficialit|superficiall|superfluou|superstitio|superstitiou|suppres|suppressio|surrende|susceptibl|suspec|suspicio|suspicion|suspiciou|suspiciousl|swagge|swampe|sweat|swelle|swellin|swindl|swip|swolle|sympto|symptom|syndrom|tabo|tack|tain|tainte|tampe|tangl|tangle|tangle|tan|tanke|tank|tantru|tard|tarnis|tarnishe|tarnishe|tarnishin|tattere|taun|tauntin|tauntingl|taunt|tau|tawdr|taxin|teas|teasingl|tediou|tediousl|temerit|tempe|tempes|temptatio|tendernes|tens|tensio|tentativ|tentativel|tenuou|tenuousl|tepi|terribl|terriblenes|terribl|terro|terror-geni|terroris|terroriz|testil|test|tetchil|tetch|thankles|thicke|thirs|thorn|thoughtles|thoughtlessl|thoughtlessnes|thras|threa|threate|threatenin|threat|threesom|thro|throbbe|throbbin|throb|throttl|thu|thumb-dow|thumbs-dow|thwar|time-consumin|timi|timidit|timidl|timidnes|tin-|tingle|tinglin|tire|tiresom|tirin|tiringl|toi|tol|top-heav|toppl|tormen|tormente|torren|tortuou|tortur|torture|torture|torturin|torturou|torturousl|totalitaria|touch|toughnes|tou|toute|tout|toxi|traduc|traged|tragi|tragicall|traito|traitorou|traitorousl|tram|trampl|transgres|transgressio|tra|trape|trappe|tras|trashe|trash|traum|traumati|traumaticall|traumatiz|traumatize|travestie|travest|treacherou|treacherousl|treacher|treaso|treasonou|tric|tricke|tricker|trick|trivia|trivializ|troubl|trouble|troublemake|trouble|troublesom|troublesomel|troublin|troublingl|truan|tumbl|tumble|tumble|tumultuou|turbulen|turmoi|twis|twiste|twist|two-face|two-face|tyrannica|tyrannicall|tyrann|tyran|ug|uglie|uglies|uglines|ugl|ulterio|ultimatu|ultimatum|ultra-hardlin|un-viewabl|unabl|unacceptabl|unacceptablel|unacceptabl|unaccessibl|unaccustome|unachievabl|unaffordabl|unappealin|unattractiv|unauthenti|unavailabl|unavoidabl|unbearabl|unbearablel|unbelievabl|unbelievabl|uncarin|uncertai|uncivi|uncivilize|unclea|unclea|uncollectibl|uncomfortabl|uncomfortabl|uncomf|uncompetitiv|uncompromisin|uncompromisingl|unconfirme|unconstitutiona|uncontrolle|unconvincin|unconvincingl|uncooperativ|uncout|uncreativ|undecide|undefine|undependabilit|undependabl|undercu|undercut|undercuttin|underdo|underestimat|underling|undermin|undermine|undermine|underminin|underpai|underpowere|undersize|undesirabl|undetermine|undi|undignifie|undissolve|undocumente|undon|undu|uneas|uneasil|uneasines|uneas|uneconomica|unemploye|unequa|unethica|uneve|uneventfu|unexpecte|unexpectedl|unexplaine|unfairl|unfaithfu|unfaithfull|unfamilia|unfavorabl|unfeelin|unfinishe|unfi|unforesee|unforgivin|unfortunat|unfortunatel|unfounde|unfriendl|unfulfille|unfunde|ungovernabl|ungratefu|unhappil|unhappines|unhapp|unhealth|unhelpfu|unilateralis|unimaginabl|unimaginabl|unimportan|uninforme|uninsure|unintelligibl|unintelligil|unipola|unjus|unjustifiabl|unjustifiabl|unjustifie|unjustl|unkin|unkindl|unknow|unlamentabl|unlamentabl|unlawfu|unlawfull|unlawfulnes|unleas|unlicense|unlikel|unluck|unmove|unnatura|unnaturall|unnecessar|unneede|unnerv|unnerve|unnervin|unnervingl|unnotice|unobserve|unorthodo|unorthodox|unpleasan|unpleasantrie|unpopula|unpredictabl|unprepare|unproductiv|unprofitabl|unprov|unprove|unprove|unprove|unprovin|unqualifie|unrave|unravele|unreachabl|unreadabl|unrealisti|unreasonabl|unreasonabl|unrelentin|unrelentingl|unreliabilit|unreliabl|unresolve|unresponsiv|unres|unrul|unsaf|unsatisfactor|unsavor|unscrupulou|unscrupulousl|unsecur|unseeml|unsettl|unsettle|unsettlin|unsettlingl|unskille|unsophisticate|unsoun|unspeakabl|unspeakablel|unspecifie|unstabl|unsteadil|unsteadines|unstead|unsuccessfu|unsuccessfull|unsupporte|unsupportiv|unsur|unsuspectin|unsustainabl|untenabl|unteste|unthinkabl|unthinkabl|untimel|untouche|untru|untrustworth|untruthfu|unusabl|unusabl|unuseabl|unuseabl|unusua|unusuall|unviewabl|unwante|unwarrante|unwatchabl|unwelcom|unwel|unwield|unwillin|unwillingl|unwillingnes|unwis|unwisel|unworkabl|unworth|unyieldin|upbrai|upheava|uprisin|uproa|uproariou|uproariousl|uproarou|uproarousl|uproo|upse|upsetin|upset|upsettin|upsettingl|urgen|useles|usur|usurpe|utterl|vagran|vagu|vaguenes|vai|vainl|vanit|vehemen|vehementl|vengeanc|vengefu|vengefull|vengefulnes|veno|venomou|venomousl|ven|vestige|ve|vexatio|vexin|vexingl|vibrat|vibrate|vibrate|vibratin|vibratio|vic|viciou|viciousl|viciousnes|victimiz|vil|vilenes|vilif|villainou|villainousl|villain|villia|villianou|villianousl|villif|vindictiv|vindictivel|vindictivenes|violat|violatio|violato|violator|violen|violentl|vipe|virulenc|virulen|virulentl|viru|vociferou|vociferousl|volatil|volatilit|vomi|vomite|vomitin|vomit|vulga|vulnerabl|wac|wai|wallo|wan|wanin|wanto|war-lik|waril|warines|warlik|warne|warnin|war|warpe|war|washed-ou|wast|waste|wastefu|wastefulnes|wastin|water-dow|watered-dow|waywar|wea|weake|weakenin|weake|weaknes|weaknesse|wearines|wearisom|wear|wedg|wee|wee|weir|weirdl|wheedl|whimpe|whin|whinin|whin|whip|whor|whore|wicke|wickedl|wickednes|wil|wildl|wile|wil|wil|wimp|winc|wobbl|wobble|wobble|wo|woebegon|woefu|woefull|womanize|womanizin|wor|worrie|worriedl|worrie|worrie|worrisom|worr|worryin|worryingl|wors|worse|worsenin|wors|worthles|worthlessl|worthlessnes|woun|wound|wrangl|wrat|wrea|wreake|wreak|wrec|wres|wrestl|wretc|wretche|wretchedl|wretchednes|wrinkl|wrinkle|wrinkle|wri|wrippe|wrippin|writh|wron|wrongfu|wrongl|wrough|yaw|za|zappe|zap|zealo|zealou|zealousl|zombie)";

public float Sentiment(String text) 
{

  float score = 0;
  int factor = 1;   
  int count = 0;
  text = text.replaceAll("((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", "");
  text = text.replace("'", "");
  text = text.replaceAll("@\\w+", "");
  text = text.replaceAll("RT+", "");
  text = text.replace("\\.+", ".");
  text = text.replace("!!+", "!");
  text = text.replace("??+", "?");
  text = text.replace("#\\w+", "");

  Pattern negation = Pattern.compile(negations, Pattern.CASE_INSENSITIVE);
  Matcher m_negation = negation.matcher(text);

  Pattern positive = Pattern.compile(pos, Pattern.CASE_INSENSITIVE);
  Matcher m_positive = positive.matcher(text);

  Pattern negative = Pattern.compile(neg, Pattern.CASE_INSENSITIVE);
  Matcher m_negative = negative.matcher(text);


  //println(text);
  if (m_negation.find())
  {
    factor = -1;
  }
  while (m_positive.find ())
  {
    score++;

    //println(score);
  }

  while (m_negative.find ()) 
  {
    score--;
    //println(score);
  }

  score = (score/text.length())* factor;
  s_max = max(score, s_max);
  s_min = min(score, s_min);
  float mapped_score = map(score, s_min, s_max, 0, 255);
  //println(score+"  "+mapped_score);
  return mapped_score;
  //return score*10;
}

  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "--full-screen", "--bgcolor=#666666", "--stop-color=#cccccc", "Emotion_Stream_Map_2" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
