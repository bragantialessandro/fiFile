/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bragantialessandro_FIFile;
import java.util.ArrayList;
/**
 *
 * @author braganti.alessandro
 */
public class StatoStagione {
    public String idSquadraScelta;
    public String nomeSquadraScelta;
    public int giornataAttuale = 1;
    public Partita ultimaPartita;
    public ArrayList<Squadra> classifica = new ArrayList<>();
    public ArrayList<Partita> calendario = new ArrayList<>();
}
