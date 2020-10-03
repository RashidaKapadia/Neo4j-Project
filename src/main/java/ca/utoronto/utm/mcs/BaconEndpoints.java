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


public class BaconEndPoints implements HttpHandler {
    private Driver driver;
    private String uriDb;

    /* Establish driver of the db */
    public BaconEndPoints(){
        uriDb = "bolt://localhost:7687";
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","12312"));
    }

    @Override
    public void handle(HttpExchange r) throws IOException {
        if (r.getRequestMethod().equals("GET")){
            try {
                URI path = exchange.getRequestURI();
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

        try (Session session = driver.session())
        {
        	try (Transaction tx = session.beginTransaction()) {
        		Result node_result = tx.run("MATCH (actor: Actor) where actor.actorId = $actorId"
                 + " OPTIONAL MATCH (actor: Actor)-[r:ACTED_IN]->(movie: Movie)"
                 + " RETURN actor.name as name, actor.actorId as actorId, movie.movieId as movieId",
                parameters("actorId", deserialized.getString("actorId")));

    }


}
