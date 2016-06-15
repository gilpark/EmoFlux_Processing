import twitter4j.util.*;
import twitter4j.*;
import twitter4j.management.*;
import twitter4j.api.*;
import twitter4j.conf.*;
import twitter4j.json.*;
import twitter4j.auth.*;

int cnt2;
String date = "";
class Getstream
{	
  ///////////////////// set your twitter dev info ////////////////////////////////
  /// This is where you enter your Oauth info
  String OAuthConsumerKey = "";
  String OAuthConsumerSecret = "";
  // This is where you enter your Access Token info
  String AccessToken = "";
  String AccessTokenSecret = "";

  AccessToken loadAccessToken() 

  {
    return new AccessToken(AccessToken, AccessTokenSecret);
  }
  ////////////////////////////////////////////////////////////////////////////////
  //initialize twitter stream	
  TwitterStream twitter = new TwitterStreamFactory().getInstance();




  double[][] loc = null;
  String[] keywords = {
    "happy", "sad",
  };
  String[] lang = {
    "en",
  };
  Getstream()  
  {  
    connectTwitter();
    twitter.addListener(listener);
  }


  // Initial connection
  void connectTwitter() 
  {
    twitter.setOAuthConsumer(OAuthConsumerKey, OAuthConsumerSecret);
    AccessToken accessToken = loadAccessToken();
    twitter.setOAuthAccessToken(accessToken);
  }

  //search tweets
  void run()
  {    
    //twitter.sample();
    getbox(41.880, -87.636);
    search(keywords, loc);
    //loc = {{-129.9023, 22.7964},{-64.5996, 51.1518}};
    //search(loc);
  }


  void getbox(double lat, double lon) { //setting bounding box


    double lon1 = lon - .8;
    double lon2 = lon + .8;
    double lat1 = lat - .8;
    double lat2 = lat + .8;
    //double box[][] = {{lon1, lat1}, {lon2, lat2}};
    double box[][] = {
      {
        -129.9023, 22.7964
      }
      , {
        -64.5996, 51.1518
      }
    };
    loc = box;
    println("1-lat,lon : "+ lat1+ " , "+lon1+" 2-lat,lon : "+ lat2+ " , "+lon2);
  }

  void search(String[] keywords, double[][] loc)
  {
  //TODO:: narrow it down.
    FilterQuery query = new FilterQuery();
    query.locations(loc);
    query.track(keywords);
   // query.language(lang);
    println(query);
    twitter.filter(query);
  }
  void search(double[][] loc)
  {

    FilterQuery query = new FilterQuery();
    query.locations(loc);
    twitter.filter(query);
  }
  void shutdown()
  {
    twitter.shutdown();
  }

  // This listens for new tweet
  StatusListener listener = new StatusListener() 
  {

    void onStatus(Status status) {
      //more info -> http://twitter4j.org/javadoc/twitter4j/Status.html
      String name = status.getUser().getScreenName();
      String msg = status.getText();
      GeoLocation loc = status.getGeoLocation();
      Place place = status.getPlace();
      Date time = status.getCreatedAt();
     // String lang = status.getLang();
     date = time.toString();

      //fillter only english tweets
      if (lang.equals("en")) {
        // add tweets in the field
        //flowfield.addtweet(loc, msg);
        if(cnt2%15==0){
        flux.addTweet(loc, msg, name);
        //println(cnt2);
        }
        cnt2++;
      }
      if(cnt2%15==0){
        flux.addTweet(loc, msg, name);
        //println(cnt2);
        }
        cnt2++;
    }

    void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
      //System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
    }
    void onTrackLimitationNotice(int numberOfLimitedStatuses) {
      // System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
    }
    void onScrubGeo(long userId, long upToStatusId) {
      //System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
    }
//    void onStallWarning(StallWarning warning) {
//      //System.out.println("Got stall warning:" + warning);
//    }

    void onException(Exception ex) {
      //ex.printStackTrace();
    }
  };
}

