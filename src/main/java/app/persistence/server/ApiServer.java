package app.persistence.server;

import app.persistence.apis.MovieAPI;
import app.persistence.daos.MovieDAO;
import app.persistence.entities.Movie;
import app.persistence.enums.HibernateConfigState;
import app.persistence.services.MovieConverter;
import app.persistence.utility.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

public class ApiServer {

    public static void main(String[] args) {
        // Initialisering af DAO
        JsonUtil jsonUtil = new JsonUtil();
        MovieDAO movieDAO = MovieDAO.getInstance(HibernateConfigState.NORMAL);

        // Her opretter jeg en ObjectMapper som jeg bruger til at serialisere data til JSON
        ObjectMapper mapper = new ObjectMapper();

        // Her opretter jeg en Javalin instans
        Javalin app = Javalin.create().start(7070);

        app.get("/moviesbyinstructor/{instructor}", ctx -> {
            try {
                // Hent instruktørens navn fra URL-parameteren
                String instructorName = ctx.pathParam("instructor");

                // Hent listen af film baseret på instruktørens navn
                List<Movie> moviesOfInstructor = movieDAO.getMoviesByDirector(instructorName);

                // Her tjekker jeg om der findes nogle film instrueret af den angivne instruktør
                if (moviesOfInstructor.isEmpty()) {
                    // Hvis der ikke findes nogen film, returnér 404 og en besked
                    ctx.status(404).result("Ingen film fundet instrueret af instruktøren: " + instructorName);
                } else {
                    // Hvis der findes film, konverter dem til JSON og returnér som svar
                    // Konverter listen af Movie-objekter til en liste af MovieAPI-objekter
                    List<MovieAPI> movieAPIS = moviesOfInstructor.stream()
                            .map(MovieConverter::convertToMovieAPI)
                            .toList();

                    // Her konverterer jeg listen af MovieAPIs til JSON og returnérer den
                    String jsonResponse = jsonUtil.convertListOfMoviesToJson(movieAPIS);
                    ctx.contentType("application/json");
                    ctx.result(jsonResponse);
                }
            } catch (Exception e) {
                // Hvis der opstår en fejl, returnér en 500 status og en fejlbesked
                ctx.status(500).result("Fejl ved konvertering af film til JSON: " + e.getMessage());
            }
        });

        app.get("/moviesbyactor/{actor}", ctx -> {
            try {
                String actorName = ctx.pathParam("actor");
                List<Movie> moviesOfActor = movieDAO.getMoviesByActor(actorName);

                if (moviesOfActor.isEmpty()) {
                    ctx.status(404).result("Der blev ikke fundet nogle film med skuespilleren: " + actorName);
                } else {
                    // Her filtrerer jeg film med gyldige release datoer og sorterer dem
                    List<Movie> sortedMovies = moviesOfActor.stream()
                            .filter(movie -> movie.getReleaseDate() != null && !movie.getReleaseDate().isEmpty())
                            .sorted(Comparator.comparing(movie -> LocalDate.parse(movie.getReleaseDate())))
                            .toList();

                    List<MovieAPI> movieAPIS = sortedMovies.stream()
                            .map(MovieConverter::convertToMovieAPI)
                            .toList();

                    String jsonResponse = jsonUtil.convertListOfMoviesToJson(movieAPIS);
                    ctx.contentType("application/json");
                    ctx.result(jsonResponse);
                }
            } catch (DateTimeParseException e) {
                ctx.status(500).result("Fejl ved parsing af release date: " + e.getMessage());
            } catch (Exception e) {
                ctx.status(500).result("Fejl ved konvertering af film til JSON: " + e.getMessage());
            }
        });

        app.get("/movies/all", ctx -> {
            try {
                String pageParam = ctx.queryParam("page");
                String sizeParam = ctx.queryParam("size");

                int page = (pageParam != null) ? Integer.parseInt(pageParam) : 0;
                int size = (sizeParam != null) ? Integer.parseInt(sizeParam) : 20;

                List<Movie> allMovies = movieDAO.getMovies(page, size);
                List<MovieAPI> movieAPIS = allMovies.stream()
                        .map(MovieConverter::convertToMovieAPI)
                        .toList();

                ctx.json(movieAPIS);
            } catch (NumberFormatException e) {
                ctx.status(400).result("Ugyldige værdier for page eller size");
            } catch (Exception e) {
                ctx.status(500).result("Der opstod en fejl: " + e.getMessage());
            }
        });

        app.get("/movies/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Movie movie = movieDAO.findById((long) id);
            if (movie != null) {
                ctx.json(movie);
            } else {
                ctx.status(404).result("Filmen blev ikke fundet med ID: " + id);
            }
        });

        app.get("/movies/imdb/{imdbId}", ctx -> {
            try {
                String imdbId = ctx.pathParam("imdbId");
                Movie movie = movieDAO.findByImdbId(Long.valueOf(imdbId));

                if (movie != null) {
                    MovieAPI movieAPI = MovieConverter.convertToMovieAPI(movie);
                    String jsonResponse = jsonUtil.convertMovieToJson(movieAPI);
                    ctx.contentType("application/json");
                    ctx.result(jsonResponse);
                } else {
                    ctx.status(404).result("Filmen blev ikke fundet med IMDB ID: " + imdbId);
                }
            } catch (Exception e) {
                ctx.status(500).result("Der opstod en fejl" + e.getMessage());
            }
        });

        app.get("/movies/genre/{genre}", ctx -> {
            String pageParam = ctx.queryParam("page");
            String sizeParam = ctx.queryParam("size");

            int page = (pageParam != null) ? Integer.parseInt(pageParam) : 0;
            int size = (sizeParam != null) ? Integer.parseInt(sizeParam) : 20;
            String genre = ctx.pathParam("genre");
            List<Movie> movies = movieDAO.getMoviesByGenreForAPIServer(genre, page, size);
            List<MovieAPI> movieAPIS = movies.stream()
                    .map(MovieConverter::convertToMovieAPI)
                    .toList();
            ctx.json(movieAPIS);
        });

        app.get("/movies/rating/{rating}", ctx -> {
            String pageParam = ctx.queryParam("page");
            String sizeParam = ctx.queryParam("size");

            int page = (pageParam != null) ? Integer.parseInt(pageParam) : 0;
            int size = (sizeParam != null) ? Integer.parseInt(sizeParam) : 20;
            double rating = Double.parseDouble(ctx.pathParam("rating"));
            List<Movie> movies = movieDAO.getMoviesByRatingForAPIServer(rating, page, size);
            List<MovieAPI> movieAPIS = movies.stream()
                    .map(MovieConverter::convertToMovieAPI)
                    .toList();
            ctx.json(movieAPIS);
        });

        app.get("/movies/year/{year}", ctx -> {
            String pageParam = ctx.queryParam("page");
            String sizeParam = ctx.queryParam("size");

            int page = (pageParam != null) ? Integer.parseInt(pageParam) : 0;
            int size = (sizeParam != null) ? Integer.parseInt(sizeParam) : 20;
            int year = Integer.parseInt(ctx.pathParam("year"));
            List<Movie> movies = movieDAO.getMoviesByReleaseYearForAPIServer(year, page, size);
            List<MovieAPI> movieAPIS = movies.stream()
                    .map(MovieConverter::convertToMovieAPI)
                    .toList();
            ctx.json(movieAPIS);
        });
    }
}
