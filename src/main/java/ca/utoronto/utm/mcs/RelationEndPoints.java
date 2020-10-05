package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;

import static org.neo4j.driver.Values.parameters;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RelationEndPoints implements HttpHandler {
    private Driver driver;
    private String uriDb;

    /* Establish driver of the db */
    public RelationEndPoints(){
        driver = Connect.getDriver();
    }

    @Override
    public void handle(HttpExchange r) throws IOException {
        if (r.getRequestMethod().equals("PUT")){
            try{
                addRelationship(r);
            }catch (Exception e) {
                e.printStackTrace();
            }
        } else if (r.getRequestMethod().equals("GET")){
            try {
                hasRelationship(r);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            r.sendResponseHeaders(500, -1);
        }
    }

    public void addRelationship(HttpExchange r) throws IOException, JSONException {
        // Convert Body to JSON Object
        String actorId = "";
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
        //actorId
        if (deserialized.has("actorId")){
            actorId = deserialized.getString("actorId");
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

        int c = create(actorId, movieId);
        if (c==0)
            r.sendResponseHeaders(500, -1);
        
        r.sendResponseHeaders(200, 0);
        OutputStream os = r.getResponseBody();
        os.write("".getBytes());
        os.close();
    }

    private int create(String actorId, String movieId){
        Result result;
        // check if movieId already exist
        // TO: Fix the Cypher command
        try (Session session = driver.session()){
            session.writeTransaction(tx -> tx.run("MATCH (actor:Actor) where actor.id = $actorId"
                            + " MATCH (movie:Movie) where movie.id = $movieId"
                            + " MERGE (actor)-[r:ACTED_IN]-(movie) RETURN type(r) as type",
                    parameters("actorId", actorId, "movieId", movieId)));
            session.close();
            return(1);
        } catch (Exception e) {
            e.printStackTrace();
            return(0);
        }
    }

    public void hasRelationship(HttpExchange r) throws IOException, JSONException{
        String response = "";
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        try (Session session = driver.session())
        {
            String actorId = deserialized.getString("actorId");
            String movieId = deserialized.getString("movieId");
        	try (Transaction tx = session.beginTransaction()) {
        		Result node_result = tx.run("MATCH (actor:Actor) where actor.id = $actorId"
                    + " MATCH (movie:Movie) where movie.id = $movieId"
                    + " RETURN EXISTS((actor)-[:ACTED_IN]-(movie))",
                parameters("actorId", actorId, "movieId", movieId));

                // If the actor is not found
                if (node_result.hasNext() == false) {
                 r.sendResponseHeaders(404, -1);
                    return;
                } 
                else{
                    Record rec = node_result.next();
                    boolean hasRelationship = rec.get("EXISTS((actor)-[:ACTED_IN]-(movie))").asBoolean();
                    // create the json response
                    response = "";
                    String info = "{ \"actorId\" : \""  + actorId + "\", \"movieId\" : \"" + movieId + "\", \"hasRelationship\" : " + hasRelationship;
                    
                    response = response.concat(info);
                    // replace the last comma and add the closing bracket
                    response = response.replaceAll(",$", "");
                    response = response.concat("}");
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

