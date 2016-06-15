class TStatus {

  String message;
  String date;
  String userName;



  boolean isHovered = false;
  float r = 4;
  boolean isDisplayed = false;
  String country;
  float x;
  float y;
  float m;
  float currSecond = 0;
  float sz;
  PVector spos;
  int age = 400;
  int age_max = age;

  Label label;
  float emo;

  TStatus (String message_, String userName_, PVector temp_spos, float _emo_val) {
    message = message_;
    //date = date_; 
    userName = userName_;
    emo = _emo_val;
    spos = new PVector(temp_spos.x, temp_spos.y);

    // create label
    label = new Label(message, emo);
  }


  void display() {


    // ScreenPosition loc = map.getScreenPosition(new Location(x, y));

    if (!isHovered) {
      //fill(80, 247, 184, 30);
      //fill(80, 247, 184,map(emo,-10,input_range,0,100));
      colorMode(RGB, 100);
      float i = map(emo, -input_range, input_range, 0, 100);
      float j = map(emo, -input_range, input_range, 95, 100);
      float k = map(age, age_max, 0, 100, 20);
      fill(i, j, 10, k);
      colorMode(RGB, 255);
    } else {
      fill(255);
    }

    noStroke();
    // strokeWeight(random(10));

    ellipseMode(CENTER);
    float s = map.getZoom();
    //println(s);
    float k = s/2;
    float t = s/4;

    int currSize=10;

    float newsize = map(age, age_max, 0, (sz-k)+abs(emo), 0);

    ellipse(spos.x, spos.y, newsize, newsize);



    // set isHovered back to false to prepare for the next loop, in case the mouse is no longer hovering over the Tweet
    isHovered = false;

    age--;
  }


  boolean onMouseOver(float mx, float my) {
    //ScreenPosition loc = map.getScreenPosition(new Location(x, y));
    float s = map.getZoom();
    float k = s*0.5;
    float t = s-k-4;
    if ((mx > 0) && (my > 0)) {
      if (dist(mx, my, spos.x, spos.y) < t) {

        isHovered = true;

        // display label on hover
        //label.display(mx, my);
        label.display();
        int labelx = int(width * .2);
        int labely = int(height*.8);
        stroke(255);
        strokeWeight(1);
        line(spos.x, spos.y, labelx + 300,labely- 200);
        line(labelx + 300,labely- 200, labelx + 270,labely-15);
        ellipse(labelx + 270,labely-15,8,8);
        noStroke();
        return true;
      }
    }
    return false;
  }
}

