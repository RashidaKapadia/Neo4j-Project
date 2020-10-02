package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;

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

public class ActorEndPoints implements HttpHandler {
    private Driver driver;
    private String uriDb;

    /* Establish driver of the db */
    public ActorEndPoints(){
        uriDb = "bolt://localhost:7687";
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","12312"));
    }

    @Override
    public void handle(HttpExchange r) throws IOException {
        if (r.getRequestMethod().equals("PUT")){
            try{
                addActor(r);
            }catch (Exception e) {
                e.printStackTrace();
            }
        } else if (r.getRequestMethod().equals("GET")){
            try {
                getActor(r);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            r.sendResponseHeaders(500, -1);
        }
    }

    public void addActor(HttpExchange r) throws IOException, JSONException {
        // Convert Body to JSON Object
        String name = "";
        String actorId = "";
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
        //actorId
        if (deserialized.has("actorId")){
            actorId = deserialized.getString("actorId");
        }else{
            r.sendResponseHeaders(400, -1);
            return;
        }

        int c = create(name, actorId);
        if (c==0)
            r.sendResponseHeaders(500, -1);
        
        r.sendResponseHeaders(200, 1);
        OutputStream os = r.getResponseBody();
        os.write("".getBytes());
        os.close();
    }

    private int create(String name, String actorId){
        Result result;
        // check if actorId already exist
        // TO: Fix the Cypher command
        try (Session session = driver.session()){
            session.writeTransaction(tx -> tx.run("MERGE (actor:Actor {actorId: $actorId})" 
                + " ON MATCH SET actor.name = $name" 
                + " ON CREATE SET actor.name = $name, actor.actorId=$actorId", 
                parameters("name", name, "actorId", actorId)));
            session.close();
            return(1);
        } catch (Exception e) {
            e.printStackTrace();
            return(0);
        }
    }
    
    public void getActor(HttpExchange r) throws IOException, JSONException{
        String response = "";
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);
        // check if actorId is provided
        // if (!body.containsKey("actorId")) {
        //     r.sendResponseHeaders(400, -1);
        //     return;
        // }

        try (Session session = driver.session())
        {
        	try (Transaction tx = session.beginTransaction()) {
        		Result node_result = tx.run("MATCH (actor: Actor)-[r:ACTED_IN]->(movie) where actor.actorId = $actorId RETURN actor.name as name, actor.actorId as actorId,"
                             + " r",
                parameters("actorId", deserialized.getString("actorId")));

                System.out.println("TEST!!!! " + node_result + "\n");
                // If the actor is not found
                if (node_result.hasNext() == false) {
                 r.sendResponseHeaders(404, -1);
                    return;
                } 
                else{
                    List<Actor> actors = new ArrayList<Actor>();
                    while (node_result.hasNext()) {
                        Record rec = node_result.next();
                        //Set up our response in a JSON format
                        Actor actor = new Actor(rec.get("name").asString(), rec.get("actorId").asString());
                        actors.add(actor);
                    }
                    // create the json response
                    response = "[";
                    for(Actor a : actors){
                    // create the actor info and concat it to the response
                    String actor_info = "{ \"name\" : " + a.name + ", \"actorId\" : " + a.actorId + " + "," },";
                    response = response.concat(actor_info);
                    }
                    // replace the last comma and add the closing bracket
                    response = response.replaceAll(",$", "");
                    response = response.concat("]");
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

/*
session.writeTransaction(tx -> tx.run("MERGE (actor:Actor {actorId: $actorId})" 
+ "ON MATCH SET actor.name = $name" 
+ "ON CREATE SET actor.name = $name, actor.actorId=$actorId", 
        parameters("name", name, "actorId", actorId)));
        */