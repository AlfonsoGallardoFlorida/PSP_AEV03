package es.florida.avaluable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.json.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class ClaseController {

	// Directori de pel·lícules
	static File directori = new File("./pelis");
	static String[] pelis = directori.list(new FiltreExtensio(".txt"));

	/**
	 * Mètode POST per afegir un nou usuari.
	 *
	 * @param stringJSON Dades de l'usuari en format JSON.
	 * @return ResponseEntity amb l'estat de l'operació.
	 */
	@PostMapping("APIpelis/nouUsuari")
	ResponseEntity<String> postBodyNouUsuari(@RequestBody String stringJSON) {
		JSONObject obj = new JSONObject(stringJSON);
		try {
			String usuari = (String) obj.get("usuari");
			if (usuari == null || usuari.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			}
			if (!usuariAutoritzat(usuari)) {
				// Creacio del directori en cas de que no existeisca
				File directoriAutoritzats = new File("./autoritzats/");
				if (!directoriAutoritzats.exists()) {
					directoriAutoritzats.mkdirs();
				}

				// Creacio del fitxer en cas de que no existeisca
				File fitxerAutoritzats = new File("./autoritzats/autoritzats.txt");
				if (!fitxerAutoritzats.exists()) {
					fitxerAutoritzats.createNewFile();
				}
				FileWriter fw = new FileWriter(fitxerAutoritzats, true);
				if (fitxerAutoritzats.length() != 0) {
					fw.write("\n");
				}

				fw.write("Nom usuari:" + usuari);
				fw.close();

				return ResponseEntity.noContent().header("Content-Length", "0").build();
			} else {
				return ResponseEntity.status(HttpStatus.CONFLICT).header("Content-Length", "0").build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	/**
	 * Mètode POST per afegir una nova pel·lícula.
	 *
	 * @param stringJSON Dades de la pel·lícula en format JSON.
	 * @return ResponseEntity amb l'estat de l'operació.
	 */
	@PostMapping("APIpelis/novaPeli")
	ResponseEntity<String> postBodyNovaPeli(@RequestBody String stringJSON) {
		JSONObject obj = new JSONObject(stringJSON);
		try {
			String titol = (String) obj.get("titol");
			if (titol == null || titol.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			}
			
			String usuari = (String) obj.get("usuari");
			if (usuariAutoritzat(usuari)) {
				// Creacio del directori en cas de que no existeisca
				if (!directori.exists()) {
					directori.mkdirs();
				}
				pelis = directori.list(new FiltreExtensio(".txt"));

				for (String peli : pelis) {
					try (BufferedReader br = new BufferedReader(new FileReader("./pelis/" + peli))) {
						String titolExistent = br.readLine().split(":")[1].trim();

						if (titolExistent.equals(titol)) {
							return ResponseEntity.status(HttpStatus.CONFLICT).build();
						}
					}
				}

				int id = pelis.length + 1;
				File fitxer = new File("./pelis/" + id + ".txt");
				if (!fitxer.exists()) {
					if (fitxer.createNewFile()) {
						FileWriter fw = new FileWriter("./pelis/" + id + ".txt", true);
						fw.write("Titol:" + titol);
						fw.close();
					}
				}
				return ResponseEntity.noContent().header("Content-Length", "0").build();
			} else {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Content-Length", "0").build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	@PostMapping("APIpelis/novaRessenya")
	ResponseEntity<String> postBodyNovaRessenya(@RequestBody String stringJSON) {
		JSONObject obj = new JSONObject(stringJSON);

		try {
			// Obtenció de les dades de la ressenya des del JSON
			String usuari = (String) obj.get("usuari");
			String id = (String) obj.get("id");
			String novaRessenya = (String) obj.get("ressenya");
			// Valicacio de la ressenya
			if (novaRessenya == null || novaRessenya.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			}

			// Verificació de l'existència del fitxer de la pel·lícula
			File fitxer = new File("./pelis/" + id + ".txt");
			if (!fitxer.exists()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}

			if (usuariAutoritzat(usuari)) {
				List<String> ressenyes = new ArrayList<>();
				boolean usuariHaRessenyat = false;

				try (BufferedReader br = new BufferedReader(new FileReader(fitxer))) {
					String linia;
					while ((linia = br.readLine()) != null) {
						String[] parts = linia.split(":");
						if (parts.length > 1 && parts[0].equals(usuari)) {
							ressenyes.add(usuari + ":" + novaRessenya);
							usuariHaRessenyat = true;
						} else {
							ressenyes.add(linia);
						}
					}
				}

				// Afegir la nova ressenya si l'usuari no ha ressenyat prèviament
				if (!usuariHaRessenyat) {
					ressenyes.add(usuari + ":" + novaRessenya);
				}

				// Actualització del fitxer amb les ressenyes
				try (FileWriter fw = new FileWriter(fitxer, false)) {
					for (String ressenya : ressenyes) {
						fw.write(ressenya + "\n");
					}
				}

				try (FileWriter fw = new FileWriter(fitxer, false)) {
					for (int i = 0; i < ressenyes.size(); i++) {
						fw.write(ressenyes.get(i));
						if (i < ressenyes.size() - 1) {
							fw.write("\n");
						}
					}
				}

				return ResponseEntity.noContent().header("Content-Length", "0").build();
			} else {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Content-Length", "0").build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	/**
	 * Mètode GET per obtenir informació sobre les pel·lícules. Si l'identificador
	 * és "all", retorna informació de totes les pel·lícules, sinó retorna
	 * informació específica d'una pel·lícula.
	 *
	 * @param strVariable Identificador de la pel·lícula o "all" per obtenir
	 *                    informació de totes les pel·lícules.
	 * @return ResponseEntity amb la informació de les pel·lícules o l'estat de
	 *         l'operació:
	 */
	@GetMapping("APIpelis/t")
	ResponseEntity<String> getInformacioPelis(@RequestParam(value = "id") String strVariable) {

		pelis = directori.list(new FiltreExtensio(".txt"));
		String resposta = "";

		if (strVariable.equals("all")) {
			JSONObject informacio = new JSONObject();
			JSONArray titols = new JSONArray();

			if (pelis != null) {
				for (int i = 0; i < pelis.length; i++) {
					try {
						FileReader fr = new FileReader("./pelis/" + pelis[i]);
						BufferedReader br = new BufferedReader(fr);

						JSONObject idJson = new JSONObject();
						JSONObject titolJson = new JSONObject();

						String id = pelis[i].split("\\.")[0];
						idJson.put("id", id);

						String titol = br.readLine().split(":")[1].trim();
						titolJson.put("titol", titol);

						titols.put(idJson);
						titols.put(titolJson);

						fr.close();
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			informacio.put("titols", titols);
			resposta = informacio.toString();
			System.out.println(resposta);
		} else {
			File fitxer = new File("./pelis/" + strVariable + ".txt");
			if (!fitxer.exists()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}
			try {
				FileReader fr = new FileReader(fitxer);
				BufferedReader br = new BufferedReader(fr);

				JSONObject titolJson = new JSONObject();
				JSONObject idJson = new JSONObject();
				JSONArray ressenyesJson = new JSONArray();
				String linia;

				idJson.put("id", strVariable);

				String titol = br.readLine().split(":")[1].trim();
				titolJson.put("titol", titol);

				while ((linia = br.readLine()) != null) {
					String resenya = linia.trim();
					ressenyesJson.put(resenya);
				}

				br.close();
				fr.close();

				resposta = idJson.toString() + titolJson.toString() + "ressenyes" + ressenyesJson.toString();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ResponseEntity.ok(resposta);
	}

	/**
	 * Verifica si l'usuari està autoritzat.
	 *
	 * @param usuari Nom de l'usuari a verificar.
	 * @return true si l'usuari està autoritzat, false altrament.
	 */
	@SuppressWarnings("resource")
	private boolean usuariAutoritzat(String nomUsuari) {
		File directoriAutoritzats = new File("./autoritzats/");
		File fitxerAutoritzats = new File("./autoritzats/autoritzats.txt");
		if (!directoriAutoritzats.exists() || !fitxerAutoritzats.exists()) {
			return false;
		}
		try {

			FileReader fr = new FileReader(fitxerAutoritzats);
			BufferedReader br = new BufferedReader(fr);

			String linia;
			while ((linia = br.readLine()) != null) {
				if (!linia.isBlank()) {
					String[] usuari = linia.split(":");
					if (nomUsuari.equals(usuari[1]))
						return true;
				}
			}
			br.close();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
}
