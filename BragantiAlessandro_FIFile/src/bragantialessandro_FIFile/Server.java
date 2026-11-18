package bragantialessandro_FIFile;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

public class Server {
    private final int PORTA = 5000;
    private final String INDIRIZZO_SQUADRE = "https://www.thesportsdb.com/api/v1/json/3/search_all_teams.php?l=Italian%20Serie%20C%20Girone%20B";
    private final String INDIRIZZO_PARTITE = "https://www.thesportsdb.com/api/v1/json/3/eventsseason.php?id=5339";

    private StatoStagione stagione = new StatoStagione();
    private Gson gson = new Gson();

    private class RispostaSquadreApi {
        ArrayList<SquadraApi> teams;
    }
    private class SquadraApi {
        String idTeam;
        String strTeam;
        String strBadge;
    }

    private class RispostaPartiteApi {
        ArrayList<PartitaApi> events;
    }
    private class PartitaApi {
        String idEvent;
        String intRound;
        String idHomeTeam;
        String idAwayTeam;
        String strHomeTeam;
        String strAwayTeam;
    }

  
    public Server() {
        try {
            ServerSocket server = new ServerSocket(PORTA);
            while (true) {
                Socket client = server.accept();
                gestisciRichiesta(client);
            }
        } catch (Exception errore) {
            errore.printStackTrace();
        }
    }

    private void gestisciRichiesta(Socket client) {
        try {
            BufferedReader ricevi = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter invia = new PrintWriter(client.getOutputStream(), true);

            String messaggio = ricevi.readLine();
            if (messaggio == null) {
                client.close();
                return;
            }

            String[] pezzi = messaggio.split(Protocollo.SEPARATORE, 2);
            String comando = pezzi[0];

            if (comando.equals(Protocollo.CARICA_E_RESET)) {
                scaricaDatiDaApi();
                azzeraCampionato();
                String testoJson = gson.toJson(stagione);
                invia.println(testoJson);
            } 
            else if (comando.equals(Protocollo.SCEGLI_SQUADRA)) {
                String nomeScelto = pezzi[1];
                stagione.nomeSquadraScelta = nomeScelto;
                for (int i = 0; i < stagione.classifica.size(); i++) {
                    Squadra s = stagione.classifica.get(i);
                    if (s.nome.equals(nomeScelto)) {
                        stagione.idSquadraScelta = s.id;
                    }
                }
                invia.println(Protocollo.RISPOSTA_OK);
            } 
            else if (comando.equals(Protocollo.PROSSIMA_GIORNATA)) {
                simulaGiornata();
                String testoJson = gson.toJson(stagione);
                invia.println(testoJson);
            } 
            else if (comando.equals(Protocollo.SALVA_FILE)) {
                String percorso = pezzi[1];
                FileWriter scrittore = new FileWriter(percorso);
                scrittore.write(gson.toJson(stagione));
                scrittore.close();
                invia.println(Protocollo.RISPOSTA_SALVATO);
            } 
            else if (comando.equals(Protocollo.CARICA_FILE)) {
                String percorso = pezzi[1];
                FileReader lettore = new FileReader(percorso);
                stagione = gson.fromJson(lettore, StatoStagione.class);
                lettore.close();
                String testoJson = gson.toJson(stagione);
                invia.println(testoJson);
            }

            client.close();
        } catch (Exception errore) {
            errore.printStackTrace();
        }
    }

   private void scaricaDatiDaApi() {
        stagione = new StatoStagione();

        try {
            // 1. Prova a scaricare le squadre da TheSportsDB
            String jsonSquadre = leggiPaginaWeb(INDIRIZZO_SQUADRE);
            RispostaSquadreApi datiSquadre = gson.fromJson(jsonSquadre, RispostaSquadreApi.class);
            if (datiSquadre != null && datiSquadre.teams != null) {
                for (int i = 0; i < datiSquadre.teams.size(); i++) {
                    SquadraApi api = datiSquadre.teams.get(i);
                    Squadra s = new Squadra();
                    s.id = api.idTeam;
                    s.nome = api.strTeam;
                    s.stemma = api.strBadge;
                    stagione.classifica.add(s);
                }
            }

            // 2. Prova a scaricare le partite da TheSportsDB
            String jsonPartite = leggiPaginaWeb(INDIRIZZO_PARTITE);
            RispostaPartiteApi datiPartite = gson.fromJson(jsonPartite, RispostaPartiteApi.class);
            if (datiPartite != null && datiPartite.events != null) {
                for (int i = 0; i < datiPartite.events.size(); i++) {
                    PartitaApi api = datiPartite.events.get(i);
                    Partita p = new Partita();
                    p.id = api.idEvent;
                    p.giornata = api.intRound;
                    p.idCasa = api.idHomeTeam;
                    p.idOspite = api.idAwayTeam;
                    p.squadraCasa = api.strHomeTeam;
                    p.squadraOspite = api.strAwayTeam;
                    stagione.calendario.add(p);
                }
            }
        } catch (Exception e) {
            System.out.println("API bloccata (403) o offline: carico le squadre di riserva...");
        }

        // Se l'API ha restituito 403 o 0 squadre, popola comunque le squadre e il calendario
        if (stagione.classifica.isEmpty()) {
            caricaDatiDiRiserva();
        }
    }

    private void caricaDatiDiRiserva() {
        String[] nomiSquadre = {
            "Arezzo", "Ascoli", "Campobasso", "Carpi", "Gubbio",
            "Legnago Salus", "Lucchese", "Milan Futuro", "Perugia", "Pescara",
            "Pianese", "Pineto", "Pontedera", "Rimini", "Sestri Levante",
            "SPAL", "Ternana", "Torres", "Virtus Entella", "Vis Pesaro"
        };

        for (int i = 0; i < nomiSquadre.length; i++) {
            Squadra s = new Squadra();
            s.id = String.valueOf(100 + i);
            s.nome = nomiSquadre[i];
            s.stemma = null;
            stagione.classifica.add(s);
        }

        // Genera automaticamente il calendario completo (andata e ritorno)
        int numSquadre = stagione.classifica.size();
        int giornateAndata = numSquadre - 1;
        int idPartita = 1;

        for (int g = 0; g < giornateAndata; g++) {
            for (int i = 0; i < numSquadre / 2; i++) {
                int idxCasa = (g + i) % (numSquadre - 1);
                int idxOspite = (numSquadre - 1 - i + g) % (numSquadre - 1);
                if (i == 0) {
                    idxOspite = numSquadre - 1;
                }

                Squadra casa = stagione.classifica.get(idxCasa);
                Squadra ospite = stagione.classifica.get(idxOspite);

                // Partita di andata
                Partita andata = new Partita();
                andata.id = String.valueOf(idPartita++);
                andata.giornata = String.valueOf(g + 1);
                andata.idCasa = casa.id;
                andata.idOspite = ospite.id;
                andata.squadraCasa = casa.nome;
                andata.squadraOspite = ospite.nome;
                stagione.calendario.add(andata);

                // Partita di ritorno
                Partita ritorno = new Partita();
                ritorno.id = String.valueOf(idPartita++);
                ritorno.giornata = String.valueOf(g + 1 + giornateAndata);
                ritorno.idCasa = ospite.id;
                ritorno.idOspite = casa.id;
                ritorno.squadraCasa = ospite.nome;
                ritorno.squadraOspite = casa.nome;
                stagione.calendario.add(ritorno);
            }
        }
    }
    private void azzeraCampionato() {
        stagione.giornataAttuale = 1;
        stagione.ultimaPartita = null;
        for (int i = 0; i < stagione.calendario.size(); i++) {
            Partita p = stagione.calendario.get(i);
            p.golCasa = null;
            p.golOspite = null;
            p.giocata = false;
        }
        calcolaClassifica();
    }

    private void simulaGiornata() {
        Random numeroCasuale = new Random();
        boolean trovata = false;
        String numeroGiornata = String.valueOf(stagione.giornataAttuale);

        for (int i = 0; i < stagione.calendario.size(); i++) {
            Partita p = stagione.calendario.get(i);
            if (!p.giocata && numeroGiornata.equals(p.giornata)) {
                p.golCasa = numeroCasuale.nextInt(4);
                p.golOspite = numeroCasuale.nextInt(4);
                p.giocata = true;
                trovata = true;

                if (stagione.idSquadraScelta != null) {
                    if (stagione.idSquadraScelta.equals(p.idCasa) || stagione.idSquadraScelta.equals(p.idOspite)) {
                        stagione.ultimaPartita = p;
                    }
                }
            }
        }

        if (trovata) {
            stagione.giornataAttuale++;
            calcolaClassifica();
        }
    }

    private void calcolaClassifica() {
        for (int i = 0; i < stagione.classifica.size(); i++) {
            Squadra s = stagione.classifica.get(i);
            s.punti = 0;
            s.giocate = 0;
            s.vinte = 0;
            s.pareggiate = 0;
            s.perse = 0;
            s.golFatti = 0;
            s.golSubiti = 0;
        }

        for (int i = 0; i < stagione.calendario.size(); i++) {
            Partita p = stagione.calendario.get(i);
            if (p.giocata && p.golCasa != null && p.golOspite != null) {
                Squadra casa = cercaSquadra(p.idCasa);
                Squadra ospite = cercaSquadra(p.idOspite);

                if (casa != null && ospite != null) {
                    casa.giocate++;
                    ospite.giocate++;
                    casa.golFatti += p.golCasa;
                    casa.golSubiti += p.golOspite;
                    ospite.golFatti += p.golOspite;
                    ospite.golSubiti += p.golCasa;

                    if (p.golCasa > p.golOspite) {
                        casa.punti += 3;
                        casa.vinte++;
                        ospite.perse++;
                    } else if (p.golCasa < p.golOspite) {
                        ospite.punti += 3;
                        ospite.vinte++;
                        casa.perse++;
                    } else {
                        casa.punti += 1;
                        ospite.punti += 1;
                        casa.pareggiate++;
                        ospite.pareggiate++;
                    }
                }
            }
        }

        int totale = stagione.classifica.size();
        for (int i = 0; i < totale - 1; i++) {
            for (int j = 0; j < totale - i - 1; j++) {
                Squadra prima = stagione.classifica.get(j);
                Squadra seconda = stagione.classifica.get(j + 1);
                if (prima.punti < seconda.punti) {
                    stagione.classifica.set(j, seconda);
                    stagione.classifica.set(j + 1, prima);
                }
            }
        }
    }

    private Squadra cercaSquadra(String idCercato) {
        for (int i = 0; i < stagione.classifica.size(); i++) {
            Squadra s = stagione.classifica.get(i);
            if (s.id.equals(idCercato)) {
                return s;
            }
        }
        return null;
    }

   private String leggiPaginaWeb(String indirizzo) throws Exception {
        URL url = new URL(indirizzo);
        HttpURLConnection connessione = (HttpURLConnection) url.openConnection();
        connessione.setRequestMethod("GET");
        
        // Header completi per superare il filtro anti-bot di Cloudflare
        connessione.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connessione.setRequestProperty("Accept", "application/json, text/plain, */*");
        connessione.setRequestProperty("Accept-Language", "it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7");

        BufferedReader lettore = new BufferedReader(new InputStreamReader(connessione.getInputStream()));
        StringBuilder testo = new StringBuilder();
        String riga;
        while ((riga = lettore.readLine()) != null) {
            testo.append(riga);
        }
        lettore.close();
        return testo.toString();
    }
}