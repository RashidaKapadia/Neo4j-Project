package ca.utoronto.utm.mcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.neo4j.driver.Values.parameters;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.neo4j.driver.Transaction;

public class Utils {
    public static String convert(InputStream inputStream) throws IOException {
 
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    public static void setUp(Driver d){
        Session session = d.session();
        try {
            session.writeTransaction(tx -> tx.run("CREATE CONSTRAINT movie_name_unique ON (movie:Movie) ASSERT movie.name IS UNIQUE"));
            session.writeTransaction(tx -> tx.run("CREATE CONSTRAINT movie_id_unique ON (movie:Movie) ASSERT movie.id IS UNIQUE"));
            session.writeTransaction(tx -> tx.run("CREATE CONSTRAINT actor_id_unique ON (actor:Actor) ASSERT actor.id IS UNIQUE"));
            session.writeTransaction(tx -> tx.run("CREATE CONSTRAINT actor_name_unique ON (actor:Actor) ASSERT actor.name IS UNIQUE"));
        } catch (Exception e) {
        }
    }

    public static void teardown(Driver d){
        Session session = d.session();
        session.writeTransaction(tx -> tx.run("DROP CONSTRAINT actor_id_unique"));
        session.writeTransaction(tx -> tx.run("DROP CONSTRAINT actor_name_unique"));
        d.close();
    }
}
