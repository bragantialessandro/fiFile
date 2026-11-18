/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package bragantialessandro_FIFile;

/**
 *
 * @author braganti.alessandro
 */
public class BragantiAlessandro_FIFile {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // 1. Avvia il Server chiamando il suo costruttore in un Thread separato
        Thread threadServer = new Thread(new Runnable() {
            public void run() {
                new Server();
            }
        });
        threadServer.start();

        // 2. Aspetta mezzo secondo per dare il tempo al Server di accendersi
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }

        // 3. Apre la finestra grafica
        FormCampionato finestra = new FormCampionato();
        finestra.setLocationRelativeTo(null);
        finestra.setVisible(true);
    }
}
