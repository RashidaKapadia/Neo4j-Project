package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.*;

import static org.neo4j.driver.Values.parameters;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.neo4j.driver.Transaction;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MovieEndPoints implements HttpHandler {
    private Driver driver;
    private String uriDb;

    /* Establish driver of the db */
    public MovieEndPoints(){
        driver = Connect.getDriver();
    }

    @Override
    public void handle(HttpExchange r) throws IOException {
        if (r.getRequestMethod().equals("PUT")){
            try{
                addMovie(r);
            }catch (Exception e) {
                e.printStackTrace();
            }
        } else if (r.getRequestMethod().equals("GET")){
            try {
                getMovie(r);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            r.sendResponseHeaders(500, -1);
        }
    }

    public void addMovie(HttpExchange r) throws IOException, JSONException {
        // Convert Body to JSON Object
        String name = "";
        String movieId = "";
        JSONObject deserialized = new JSONObject();
        try{
            String body = Utils.convert(r.getRequestBody());
            deserialized = new JSONObject(body); 
        } catch (JSONException e){
            r.sendResponseHeaders(400, -1);
            return;
        } catch (IOException e){
            r.sendResponseHeaders(500, -1);
            return;
        }
        
        //Get Data from JSON
        //name
        if (deserialized.has("name")){
            name = deserialized.getString("name");
        }else{
            r.sendResponseHeaders(400, -1);
            return;
        }
        //movieId
        if (deserialized.has("movieId")){
            movieId = deserialized.getString("movieId");
        }else{
            r.sendResponseHeaders(400, -1);
            return;
        }

        int c = create(name, movieId);
        if (c==2)
            r.sendResponseHeaders(500, -1);
        else if (c==1)
            r.sendResponseHeaders(400, -1);
        else
            r.sendResponseHeaders(200, 0);
        OutputStream os = r.getResponseBody();
        os.write("".getBytes());
        os.close();
    }

    private int create(String name, String movieId){
        Result result;
        // check if movieId already exist
        // TO: Fix the Cypher command
        try (Session session = driver.session()){
            session.writeTransaction(tx -> tx.run("MERGE (movie:Movie {id: $movieId, name: $name})" 
                + " ON CREATE SET movie.name = $name, movie.id=$movieId", 
                parameters("name", name, "movieId", movieId)));
            session.close();
            return(0);
        } catch (org.neo4j.driver.exceptions.ClientException e) {
            return(1);
        }
        catch (Exception e) {
            return(2);
        }
    }

    public void getMovie(HttpExchange r) throws IOException, JSONException{
        String response = "";
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        try (Session session = driver.session())
        {
        	try (Transaction tx = session.beginTransaction()) {
        		Result node_result = tx.run("MATCH (movie: Movie) where movie.id=$movieId"
                 + " OPTIONAL MATCH (actor: Actor)-[r:ACTED_IN]->(movie: Movie)"
                 + " RETURN movie.name as name, movie.id as movieId, actor.id as actorId",
                parameters("movieId", deserialized.getString("movieId")));

                // If the actor is not found
                if (node_result.hasNext() == false) {
                 r.sendResponseHeaders(404, -1);
                    return;
                } 
                else{
                    List<Actor> actors = new ArrayList<Actor>();
                    List<Movie> movies = new ArrayList<Movie>();
                    while (node_result.hasNext()) {
                        Record rec = node_result.next();
                        //Set up our response in a JSON format
                        Actor actor = new Actor(null, rec.get("actorId").asString());
                        Movie movie = new Movie(rec.get("name").asString(), rec.get("movieId").asString());
                        actors.add(actor);
                        movies.add(movie);
                    }
                    Movie m = movies.get(0);
                    // create the json response
                    response = "";
                    String movie_info = "{ \"movieId\" : \""  + m.movieId + "\", \"name\" : \"" + m.name + "\", \"actors\" : ["; 
                    String actor_info = "";
                    for(Actor a : actors){
                        if (a.actorId != "null"){
                            actor_info =  "\"" + a.actorId +  "\",";
                        }
                        movie_info = movie_info.concat(actor_info);
                    }
                    response = response.concat(movie_info);
                    // replace the last comma and add the closing bracket
                    response = response.replaceAll(",$", "");
                    response = response.concat("]}");
                }
                // send back appropriate responses
                r.sendResponseHeaders(200, response.length());
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } 
            catch(Exception e){
                e.printStackTrace();
                r.sendResponseHeaders(400, response.length());
            }
        }        
    }
}