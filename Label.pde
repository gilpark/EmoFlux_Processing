class Label {

  
  PFont HelveticaNeue;
  String txt;
  float labelW = 250; // set maximum label width
  float labelH = 600; // set maximum label height
  int fontSize = 11;
  int leading = 18;
  int offsetX = 20;
  int offsetY = 0;
  float emo_val;
  
  int labelx = int(width * .2);
  int labely = int(height*.8);
  
  Label(String txt_, float emo) {
    txt = txt_;
    HelveticaNeue = createFont("HelveticaNeue-Thin-11.vlw",11);
    emo_val = emo;
  }

  void display(float x, float y) {

    // get text width
    textFont(HelveticaNeue);
    textSize(fontSize);
    textLeading(leading);

    // check if label would go beyond screen dims
    if (x + labelW + offsetX > width) {
      x -= labelW + offsetX;
    }

    // get text height to set rectangle size for label background
    float rectH = calculateTextHeight(txt, int(labelW), fontSize, leading);

    // draw bg
    fill(30);
    noStroke();
    //rectMode(CORNER); // note: this is the default mode. confusing b/c similar to CORNERS (plural)
    //rect(x + offsetX, y + offsetY, labelW + offsetX, rectH + 5); 

    // draw text  
    fill(218, 229, 225);
    text(txt, x + offsetX + 5, y + offsetY + 5, labelW, labelH);
  }
  void display() {

    // get text width
    textFont(HelveticaNeue);
    textSize(fontSize);
    textLeading(leading);

    // check if label would go beyond screen dims
    if (labelx + labelW + offsetX > width) {
      labelx -= labelW + offsetX;
    }

    // get text height to set rectangle size for label background
    float rectH = calculateTextHeight(txt, int(labelW), fontSize, leading);

    // draw bg
    noStroke();

    // draw text  
    fill(218, 229, 225);
    text(txt, labelx, 550, labelW, labelH);
   // text(emo_val,labelx, labely);
  }

  int calculateTextHeight(String string, int specificWidth, int fontSize, int lineSpacing) {

    String[] wordsArray;
    String tempString = "";
    int numLines = 0;
    float textHeight;

    wordsArray = split(string, " ");

    for (int i = 0; i < wordsArray.length; i++) {

      if (textWidth(tempString + wordsArray[i]) < specificWidth) {
        tempString += wordsArray[i] + " ";
      }
      else {
        tempString = wordsArray[i] + " ";
        numLines++;
      }
    }

    numLines++; //adds the last line

    textHeight = numLines * lineSpacing;
    return(round(textHeight));
  }
}

