package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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


public class BaconEndPoints implements HttpHandler {
    private Driver driver;
    private String uriDb;

    /* Establish driver of the db */
    public BaconEndPoints(){
        driver = Connect.getDriver();
    }

    @Override
    public void handle(HttpExchange r) throws IOException {
        if (r.getRequestMethod().equals("GET")){
            try {
                URI path = r.getRequestURI();
                if (path.getPath().startsWith("/api/v1/computeBaconNumber"))
                    computeBaconNumber(r);
                else
                    computeBaconPath(r);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            r.sendResponseHeaders(500, -1);
        }
    }

    public void computeBaconNumber(HttpExchange r) throws IOException, JSONException {
        String response = "";
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

      
        if (deserialized.getString("actorId").equals("nm0000102")){
            response = "{\"baconNumber\" : \"0\"}";
            r.sendResponseHeaders(200, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        try (Session session = driver.session())
        {
            // check if actorId is same as kevin Bacon  actorId
        	try (Transaction tx = session.beginTransaction()) {
                Result node_result = tx.run("MATCH (actor:Actor{id:$actorId}),(kevin:Actor{id:'nm0000102'}), p = shortestPath((actor)-[:ACTED_IN*]-(kevin))" 
                + " RETURN length(p)/2 as baconNumber",
                parameters("actorId", deserialized.getString("actorId")));
                
                // If the actor is not found
                if (node_result.hasNext() == false) {
                    r.sendResponseHeaders(404, -1);
                       return;
                } 
                else{
                       Record rec = node_result.next();
                       Integer baconNumber = rec.get("baconNumber").asInt();
                       // create the json response
                       response = "";
                       String info = "{ \"baconNumber\" : \""  + baconNumber;
                       
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
            }
        }
    }

    public void computeBaconPath(HttpExchange r) throws IOException, JSONException {
            String response = "";
            String body = Utils.convert(r.getRequestBody());
            JSONObject deserialized = new JSONObject(body);

            if (deserialized.getString("actorId").equals("nm0000102")){
                try (Transaction tx = driver.session().beginTransaction()) {
                    Result node_result = tx.run("MATCH (a:Actor {id: 'nm0000102'})-[ACTED_IN]-(m:Movie)"
                                + " RETURN m.id as movieId");
                    
                    if (node_result.hasNext() == false) {
                        r.sendResponseHeaders(404, -1);
                        return;
                    } 
                    else{
                        Record rec = node_result.next();
                        response = "{ \"baconPath\" : [{ \"actorId\" : \"nm0000102\", \"movieId\" : \"" + rec.get("movieId").asString() + "\" }],";
                        response = response.concat("\"baconNumber\" : \"0\"}");
                        r.sendResponseHeaders(200, response.length());
                        OutputStream os = r.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    }
                    
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
    
            try (Session session = driver.session())
            {
                // check if actorId is same as kevin Bacon  actorId
                try (Transaction tx = session.beginTransaction()) {
                    Result node_result = tx.run("MATCH (actor:Actor{id:$actorId}),(kevin:Actor{id:'nm0000102'}), p = shortestPath((actor)-[:ACTED_IN*]-(kevin))"
                                        + " UNWIND nodes(p) AS path"
                                        + " RETURN path.id AS ID, length(p)/2 as baconNumber",
                                        parameters("actorId", deserialized.getString("actorId")));
                    
                    // If the actor is not found
                    if (node_result.hasNext() == false) {
                        r.sendResponseHeaders(404, -1);
                           return;
                    } 
                    else{
                        //    while (node_result.hasNext()) {
                            Record rec = node_result.next();
                            Integer baconNumber = rec.get("baconNumber").asInt();

 
                           // create the json response
                           response = "";
                           String info = "{ \"baconNumber\" : \""  + baconNumber + "\", \"baconPath\":[";

                           boolean is_actor = true;
                            Record next_node;
                           while(node_result.hasNext()){
                               next_node = node_result.next();
                               if (is_actor){
                                  info = info.concat("{\"actorId\": " + rec.get(0) + ",");
                                  info = info.concat("\"movieId\": " + next_node.get(0) + "},");
                                  is_actor = false;

                               } else{
                                    info = info.concat("{\"actorId\": " + next_node.get(0) + ",");
                                    info = info.concat("\"movieId\": " + rec.get(0) + "},"); 
                                    is_actor = true;
 
                               }
                               rec = next_node;
                                

                           }
                           response = response.concat(info);
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
                }
            }
        
    }
}
