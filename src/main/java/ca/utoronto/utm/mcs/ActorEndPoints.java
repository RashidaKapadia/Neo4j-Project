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

public class ActorEndPoints implements HttpHandler {
    private Driver driver;
    private String uriDb;

    /* Establish driver of the db */
    public ActorEndPoints(){
        uriDb = "bolt://localhost:7687";
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","12312"));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("PUT")){
            try{
                addActor(exchange);
            }catch (Exception e) {
                e.printStackTrace();
            }
        } else if (exchange.getRequestMethod().equals("GET")){
            try {
                //getActor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            exchange.sendResponseHeaders(500, -1);
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


}

/*
session.writeTransaction(tx -> tx.run("MERGE (actor:Actor {actorId: $actorId})" 
+ "ON MATCH SET actor.name = $name" 
+ "ON CREATE SET actor.name = $name, actor.actorId=$actorId", 
        parameters("name", name, "actorId", actorId)));
        */