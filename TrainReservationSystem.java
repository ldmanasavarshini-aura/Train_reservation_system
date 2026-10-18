import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
        server.createContext("/", new StaticHandler());
        server.createContext("/api/book", new BookingHandler());
        server.createContext("/api/update-train", new TrainUpdateHandler());
        server.createContext("/book", new BookingHandler());
        server.createContext("/update-train", new TrainUpdateHandler());
        server.setExecutor(null); 
        System.out.println("✅ Driver-free Server active at http://localhost:8080/");
        server.start();
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) path = "/index.html";
            String contentType;
            if ("/index.html".equals(path)) contentType = "text/html; charset=UTF-8";
            else if ("/style.css".equals(path)) contentType = "text/css; charset=UTF-8";
            else if ("/script.js".equals(path)) contentType = "text/javascript; charset=UTF-8";
            else {
                sendResponse(exchange, 404, "Not found");
                return;
            }
            File file = new File(path.substring(1));
            if (!file.isFile()) {
                sendResponse(exchange, 404, "Not found");
                return;
            }
            byte[] content = new byte[(int) file.length()];
            FileInputStream input = new FileInputStream(file);
            try {
                int offset = 0;
                while (offset < content.length) {
                    int read = input.read(content, offset, content.length - offset);
                    if (read < 0) break;
                    offset += read;
                }
            } finally {
                input.close();
            }
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, content.length);
            OutputStream output = exchange.getResponseBody();
            output.write(content);
            output.close();
        }
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
                int seats = parsePositiveInt(params.get("totalSeats"));
                if (trainNum == null || trainNum.trim().isEmpty() || trainName == null || trainName.trim().isEmpty()
                        || source == null || source.trim().isEmpty() || dest == null || dest.trim().isEmpty() || seats == 0) {
                    sendResponse(exchange, 400, "ERROR: Enter valid train details and a positive seat capacity.");
                    return;
                }

                Train newTrain = new Train(trainNum, trainName, source, dest, seats);
                trainDatabase.put(trainNum, newTrain); 

                sendResponse(exchange, "SUCCESS: Train " + trainNum + " setup complete. Total capacity allocated: " + seats + " seats.");
            } else {
                sendResponse(exchange, 405, "Method not allowed");
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
                int seatsRequested = parsePositiveInt(params.get("seatCount"));
                if (passenger == null || passenger.trim().isEmpty() || trainNum == null || trainNum.trim().isEmpty() || seatsRequested == 0) {
                    sendResponse(exchange, 400, "ERROR: Enter a passenger name, train number, and positive seat count.");
                    return;
                }

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
            } else {
                sendResponse(exchange, 405, "Method not allowed");
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
            int separator = param.indexOf('=');
            if (separator >= 0) result.put(URLDecoder.decode(param.substring(0, separator), "UTF-8"),
                    URLDecoder.decode(param.substring(separator + 1), "UTF-8"));
        }
        return result;
    }

    private static int parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : 0;
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, String textResponse) throws IOException {
        sendResponse(exchange, 200, textResponse);
    }

    private static void sendResponse(HttpExchange exchange, int status, String textResponse) throws IOException {
        byte[] bytes = textResponse.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
