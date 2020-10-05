package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class App 
{
    static int PORT = 8080;
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        server.start();
        /* Actor APIs*/
        server.createContext("/api/v1/addActor", new ActorEndPoints());
        server.createContext("/api/v1/getActor", new ActorEndPoints());

        // /* Movie APIs*/
        server.createContext("/api/v1/addMovie", new MovieEndPoints());
        server.createContext("/api/v1/getMovie", new MovieEndPoints());

        // /* Relationship between Actor and Movies */
        server.createContext("/api/v1/addRelationship", new RelationEndPoints());
        server.createContext("/api/v1/hasRelationship", new RelationEndPoints());

        // /* Bacon APIc*/
        server.createContext("/api/v1/computeBaconNumber", new BaconEndPoints());
        server.createContext("/api/v1/computeBaconPath", new BaconEndPoints());

        System.out.printf("Server started on port %d...\n", PORT);
    }
}
