import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class TrainReservationSystem {

    // Structure blueprint for handling train details inside local memory array profiles
    static class Train {
        String number;
        String name;
        String source;
        String destination;
        int totalSeats;
        int bookedSeats;

        public Train(String number, String name, String source, String destination, int totalSeats) {
            this.number = number;
            this.name = name;
            this.source = source;
            this.destination = destination;
            this.totalSeats = totalSeats;
            this.bookedSeats = 0; 
        }
    }

    // Direct Collection In-Memory Hash Map storage
    private static final Map<String, Train> trainDatabase = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // Pre-populating an absolute default value record so testing is easy
        trainDatabase.put("12345", new Train("12345", "Express Rail", "Chennai", "Coimbatore", 50));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/book", new BookingHandler());
        server.createContext("/update-train", new TrainUpdateHandler());
        server.setExecutor(null); 
        System.out.println("✅ Driver-free Server active at http://localhost:8080/");
        server.start();
    }

    // --- SERVICE LAYER: DATA INPUT PROVISIONING ---
    static class TrainUpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                String trainNum = params.get("trainNumber");
                String trainName = params.get("trainName");
                String source = params.get("source");
                String dest = params.get("destination");
                int seats = Integer.parseInt(params.getOrDefault("totalSeats", "60"));

                Train newTrain = new Train(trainNum, trainName, source, dest, seats);
                trainDatabase.put(trainNum, newTrain); 

                sendResponse(exchange, "SUCCESS: Train " + trainNum + " setup complete. Total capacity allocated: " + seats + " seats.");
            }
        }
    }

    // --- SERVICE LAYER: CUSTOMER SEAT BOOKING TRANSACTION ---
    static class BookingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                String passenger = params.get("passengerName");
                String trainNum = params.get("trainNumber");
                int seatsRequested = Integer.parseInt(params.getOrDefault("seatCount", "1"));

                if (!trainDatabase.containsKey(trainNum)) {
                    sendResponse(exchange, "ERROR: The entered Train Number (" + trainNum + ") cannot be found in our schedules.");
                    return;
                }

                Train train = trainDatabase.get(trainNum);
                int currentlyAvailable = train.totalSeats - train.bookedSeats;

                if (currentlyAvailable < seatsRequested) {
                    sendResponse(exchange, "ERROR: Insufficient seat availability. You requested " + seatsRequested + " seats, but only " + currentlyAvailable + " are left.");
                    return;
                }

                train.bookedSeats += seatsRequested;
                int remainingSeats = train.totalSeats - train.bookedSeats;

                sendResponse(exchange, "SUCCESS: Successfully booked " + seatsRequested + " ticket(s) for " + passenger + " on Train " + trainNum + " (" + train.name + "). Remaining open seats left: " + remainingSeats);
            }
        }
    }

    // --- JAVA 8 STREAM STREAMING BUFFER PARSER ---
    private static Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            bos.write(buffer, 0, length);
        }
        String body = bos.toString("UTF-8");

        Map<String, String> result = new HashMap<>();
        for (String param : body.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                result.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8"));
            }
        }
        return result;
    }

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, String textResponse) throws IOException {
        byte[] bytes = textResponse.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}