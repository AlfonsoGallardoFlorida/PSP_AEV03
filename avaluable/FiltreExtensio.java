package es.florida.avaluable;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filtre per a la selecció de fitxers amb una extensió específica.
 */
public class FiltreExtensio implements FilenameFilter {
	String extensio;

	/**
	 * Constructor que inicialitza el filtre amb una extensió específica.
	 *
	 * @param extensio Extensió de fitxers a acceptar.
	 */
	public FiltreExtensio(String extensio) {
		this.extensio = extensio;
	}

	/**
	 * Mètode per determinar si un fitxer compleix el filtre d'extensió.
	 *
	 * @param dir  Directori del fitxer.
	 * @param name Nom del fitxer.
	 * @return true si el fitxer té l'extensió especificada, false altrament.
	 */
	public boolean accept(File dir, String name) {
		return name.endsWith(extensio);
	}
}