package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;

import static org.neo4j.driver.Values.parameters;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MovieEndPoints implements HttpHandler {
    private Driver driver;
    private String uriDb;

    /* Establish driver of the db */
    public MovieEndPoints(){
        uriDb = "bolt://localhost:7687";
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","12312"));
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
                //getMovie();
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
        if (c==0)
            r.sendResponseHeaders(500, -1);
        
        r.sendResponseHeaders(200, 1);
        OutputStream os = r.getResponseBody();
        os.write("".getBytes());
        os.close();
    }

    private int create(String name, String movieId){
        Result result;
        // check if movieId already exist
        // TO: Fix the Cypher command
        try (Session session = driver.session()){
            session.writeTransaction(tx -> tx.run("MERGE (movie:Movie {movieId: $movieId})" 
                + " ON MATCH SET movie.name = $name" 
                + " ON CREATE SET movie.name = $name, movie.movieId=$movieId", 
                parameters("name", name, "movieId", movieId)));
            session.close();
            return(1);
        } catch (Exception e) {
            e.printStackTrace();
            return(0);
        }
    }

}