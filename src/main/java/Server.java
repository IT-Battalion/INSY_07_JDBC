import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import model.Article;
import model.Client;
import model.Order;
import model.OrderLine;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.json.JSONArray;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * INSY Webshop Server
 */
public class Server {

    private static SessionFactory sessionFactory = null;
    /**
     * Port to bind to for HTTP service
     */
    private final int port = 8000;

    public static void main(String[] args) throws Throwable {
        Server webshop = new Server();
        webshop.start();
        System.out.println("Webshop running at http://127.0.0.1:" + webshop.port);
    }

    /**
     * Helper method to parse query paramaters
     */
    public static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }

    private static <T> List<T> loadAllData(Class<T> type, Session session) {
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<T> criteria = builder.createQuery(type);
        criteria.from(type);
        return session.createQuery(criteria).getResultList();
    }

    /**
     * Connect to the database
     */
    Session setupDB() {
        if (sessionFactory == null) {
            StandardServiceRegistry standardServiceRegistry = new StandardServiceRegistryBuilder()
                    .configure("hibernate.cfg.xml").build();
            Metadata metadata = new MetadataSources(standardServiceRegistry).getMetadataBuilder().build();
            sessionFactory = metadata.getSessionFactoryBuilder().build();
        }
        return sessionFactory.openSession();
    }

    /**
     * Startup the Webserver
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
     * Handler for listing all articles
     */
    class ArticlesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Session ses = setupDB();
            JSONArray res = new JSONArray(loadAllData(Article.class, ses));

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
            Session ses = setupDB();

            JSONArray res = new JSONArray(loadAllData(Article.class, ses));

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
            Session ses = setupDB();

            JSONArray res = new JSONArray(loadAllData(Order.class, ses));

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

            Session ses = setupDB();
            Map<String, String> params = queryToMap(t.getRequestURI().getQuery());

            int client_id = Integer.parseInt(params.get("client_id"));

            String response;
            try {
                CriteriaBuilder builder = ses.getCriteriaBuilder();
                CriteriaQuery<Client> criteria = builder.createQuery(Client.class);
                Root<Client> root = criteria.from(Client.class);
                criteria.where(builder.equal(root.get("client_id"), client_id));
                Client client = ses.createQuery(criteria).getSingleResult();

                Order order = new Order();
                order.setClient(client);
                order.setCreated_at(Timestamp.valueOf(LocalDateTime.now()));
                order.setOrderLines(new HashSet<>());


                for (int i = 1; i <= (params.size() - 1) / 2; ++i) {
                    int article_id = Integer.parseInt(params.get("article_id_" + i));
                    int amount = Integer.parseInt(params.get("amount_" + i));

                    builder = ses.getCriteriaBuilder();
                    CriteriaQuery<Article> criteria2 = builder.createQuery(Article.class);
                    Root<Article> root2 = criteria2.from(Article.class);
                    criteria2.where(builder.equal(root2.get("id"), article_id));
                    Article article = ses.createQuery(criteria2).getSingleResult();
                    int available = article.getAmount();

                    if (available < amount)
                        throw new IllegalArgumentException(String.format("Not enough items of article #%d available", article_id));

                    article.setAmount(available - amount);
                    ses.update(article);

                    OrderLine orderLine = new OrderLine();
                    orderLine.setAmount(amount);
                    orderLine.setArticle(article);
                    ses.save(orderLine);
                    order.getOrderLines().add(orderLine);
                }

                ses.save(order);
                ses.getTransaction().commit();
                response = String.format("{\"order_id\": %d}", order.getId());
            } catch (IllegalArgumentException iae) {
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
}