import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;

import org.json.*;

import java.sql.*;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * INSY Webshop Server
 */
public class Server {

    /**
     * Port to bind to for HTTP service
     */
    private int port = 8000;

    /**
     * Connect to the database
     *
     * @throws IOException
     */
    Connection setupDB() {
        Properties dbProps = new Properties();
        try {
            //Meine schöne überarbeitung ;)
            URL dbconfig = getClass().getResource("/db.properties");
            dbProps.load(new FileInputStream(dbconfig.getFile()));
            //TODO Connect to DB at the url dbProps.getProperty("url")
            Connection conn = DriverManager.getConnection(dbProps.getProperty("url"), dbProps);
            //initDatabase(conn);
            return conn;
        } catch (Exception throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    private void initDatabase(Connection connection) throws SQLException {
        InputStream in = getClass().getResourceAsStream("/webshop.sql");
        if (in != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String sql = reader.lines().collect(Collectors.joining());
            for (String sSql :
                    sql.split(";")) {
                connection.prepareStatement(sSql).executeUpdate();
            }
        }
    }

    /**
     * Startup the Webserver
     *
     * @throws IOException
     */
    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/articles", new ArticlesHandler());
        server.createContext("/clients", new ClientsHandler());
        server.createContext("/placeOrder", new PlaceOrderHandler());
        server.createContext("/orders", new OrdersHandler());
        server.createContext("/", new IndexHandler());

        server.start();
    }


    public static void main(String[] args) throws Throwable {
        Server webshop = new Server();
        webshop.start();
        System.out.println("Webshop running at http://127.0.0.1:" + webshop.port);
    }


    /**
     * Handler for listing all articles
     */
    class ArticlesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();

            JSONArray res = new JSONArray();

            //TODO read all articles and add them to res
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM articles;");
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    res.put(new JSONObject("{\"serial\": \"" + rs.getString(1) + "\", \"description\": \"" + rs.getString(2) + "\", \"price\": \"" + rs.getInt(3) + "\", \"amount\": \"" + rs.getInt(4) + "\"}"));
                }
                rs.close();
                stmt.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t, res.toString());
        }

    }

    /**
     * Handler for listing all clients
     */
    class ClientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();

            JSONArray res = new JSONArray();

            //TODO read all clients and add them to res
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM clients;");
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    res.put(new JSONObject("{\"serial\": \"" + rs.getString(1) + "\", \"name\": \"" + rs.getString(2) + "\", \"address\": \"" + rs.getString(3) + "\", \"city\": \"" + rs.getString(4) + "\", \"country\": \"" + rs.getString(5) + "\"}"));
                }
                rs.close();
                stmt.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t, res.toString());
        }

    }


    /**
     * Handler for listing all orders
     */
    class OrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();

            JSONArray res = new JSONArray();

            //TODO read all orders and add them to res
            // Join orders with clients, order lines, and articles
            // Get the order id, client name, number of lines, and total prize of each order and add them to res
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT orders.id, orders.client_id, orders.created_at, c.name, count(ol), a.price FROM orders LEFT JOIN clients c on c.id = orders.client_id LEFT JOIN order_lines ol on orders.id = ol.order_id LEFT JOIN articles a on a.id = ol.article_id group by orders.id, c.name, a.price;");
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    res.put(new JSONObject("{" +
                            "\"id\": \"" + rs.getInt(1) + "\", " +
                            "\"client_id\": \"" + rs.getInt(2) + "\", " +
                            "\"created_at\": \"" + rs.getTimestamp(3).toString() + "\", " +
                            "\"name\": \"" + rs.getString(4) + "\", " +
                            "\"count\": \"" + rs.getInt(5) + "\", " +
                            "\"price\": \"" + rs.getInt(6) + "\"" +
                            "}"));
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t, res.toString());

        }
    }


    /**
     * Handler class to place an order
     */
    class PlaceOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {

            Connection conn = setupDB();
            Map<String, String> params = queryToMap(t.getRequestURI().getQuery());

            int client_id = Integer.parseInt(params.get("client_id"));

            String response = "";
            int order_id = 1;
            try {
//TODO Get the next free order id

                //TODO Create a new order with this id for client client_id@
                PreparedStatement stmt = conn.prepareStatement("SELECT id FROM orders WHERE max(id);");
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    order_id = rs.getInt(1) + 1;
                    stmt = conn.prepareStatement("INSERT INTO orders (id, client_id) VALUES (?, ?);");
                    stmt.setInt(1, order_id);
                    stmt.setInt(2, client_id);
                }


                for (int i = 1; i <= (params.size() - 1) / 2; ++i) {
                    int article_id = Integer.parseInt(params.get("article_id_" + i));
                    int amount = Integer.parseInt(params.get("amount_" + i));


                    //TODO Get the available amount for article article_id
                    stmt = conn.prepareStatement("SELECT amount FROM articles WHERE id=?;");
                    stmt.setInt(1, article_id);
                    rs = stmt.executeQuery();
                    int available = rs.getInt(1);


                    if (available < amount)
                        throw new IllegalArgumentException(String.format("Not enough items of article #%d available", article_id));

                    //TODO Decrease the available amount for article article_id by amount
                    stmt = conn.prepareStatement("UPDATE articles SET amount=? WHERE id=?;");
                    stmt.setInt(1, available - amount);
                    stmt.setInt(2, article_id);
                    stmt.executeUpdate();

                    //TODO Insert new order line
                    stmt = conn.prepareStatement("SELECT id FROM oder_lines WHERE max(id);");
                    rs = stmt.executeQuery();
                    int orderLineID = rs.getInt(1);
                    stmt = conn.prepareStatement("INSERT INTO order_lines (id, article_id, order_id, amount) VALUES (?, ?, ?, ?);");
                    stmt.setInt(1, orderLineID);
                    stmt.setInt(2, article_id);
                    stmt.setInt(3, order_id);
                    stmt.setInt(4, amount);
                    stmt.executeUpdate();
                }

                response = String.format("{\"order_id\": %d}", order_id);
            } catch (IllegalArgumentException | SQLException iae) {
                response = String.format("{\"error\":\"%s\"}", iae.getMessage());
            }
            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t, response);


        }
    }

    /**
     * Handler for listing static index page
     */
    class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "<!doctype html>\n" +
                    "<html><head><title>INSY Webshop</title><link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/water.css@2/out/water.css\"></head>" +
                    "<body><h1>INSY Pseudo-Webshop</h1>" +
                    "<h2>Verf&uuml;gbare Endpoints:</h2><dl>" +
                    "<dt>Alle Artikel anzeigen:</dt><dd><a href=\"http://127.0.0.1:" + port + "/articles\">http://127.0.0.1:" + port + "/articles</a></dd>" +
                    "<dt>Alle Bestellungen anzeigen:</dt><dd><a href=\"http://127.0.0.1:" + port + "/orders\">http://127.0.0.1:" + port + "/orders</a></dd>" +
                    "<dt>Alle Kunden anzeigen:</dt><dd><a href=\"http://127.0.0.1:" + port + "/clients\">http://127.0.0.1:" + port + "/clients</a></dd>" +
                    "<dt>Bestellung abschicken:</dt><dd><a href=\"http://127.0.0.1:" + port + "/placeOrder?client_id=<client_id>&article_id_1=<article_id_1>&amount_1=<amount_1&article_id_2=<article_id_2>&amount_2=<amount_2>\">http://127.0.0.1:" + port + "/placeOrder?client_id=&lt;client_id>&article_id_1=&lt;article_id_1>&amount_1=&lt;amount_1>&article_id_2=&lt;article_id_2>&amount_2=&lt;amount_2></a></dd>" +
                    "</dl></body></html>";
            answerRequest(t, response);
        }
    }

    /**
     * Helper function to send an answer given as a String back to the browser
     *
     * @param t        HttpExchange of the request
     * @param response Answer to send
     */
    private void answerRequest(HttpExchange t, String response) throws IOException {
        byte[] payload = response.getBytes();
        t.sendResponseHeaders(200, payload.length);
        OutputStream os = t.getResponseBody();
        os.write(payload);
        os.close();
    }

    /**
     * Helper method to parse query paramaters
     */
    public static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }
}
