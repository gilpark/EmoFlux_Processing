class Test_input {
  PVector pos;
  float val;
  Test_input(PVector _pos, float _val) {
    val = _val;
    pos = new PVector(_pos.x, _pos.y);
  } 
  void display() {
    //fill(val);
    stroke(255,0,0,200);
    rect(pos.x, pos.y, 4, 4);
    //ellipse(pos.x, pos.y, 2, 2) ;

    noStroke(); 
  }
}

