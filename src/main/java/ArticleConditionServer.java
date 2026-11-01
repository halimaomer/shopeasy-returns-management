import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ArticleConditionServer {

    public static void main(String[] args) throws Exception {

        // Erstellt den gRPC-Server auf Port 9090
        Server server = ServerBuilder
                .forPort(9090)
                .addService(new ArticleConditionServiceImpl())
                .build();

        // Startet den gRPC-Server
        server.start();

        System.out.println("gRPC-Server läuft auf Port 9090");

        // Hält den Server am Laufen
        server.awaitTermination();
    }
}
