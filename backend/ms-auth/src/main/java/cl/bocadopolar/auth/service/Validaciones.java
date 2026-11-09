package cl.bocadopolar.auth.service;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cl.bocadopolar.auth.exception.SolicitudInvalidaException;

/** Reglas de validations.js del prototipo que no caben en una anotación. */
public final class Validaciones {

    private static final Pattern CORREO = Pattern.compile("^([a-z0-9._%+-]+)@([a-z0-9.-]+)$");
    private static final Pattern RUT = Pattern.compile("^(\\d{7,8})-([\\dK])$");

    private Validaciones() {
    }

    /** El correo debe terminar en .com o .cl y ese final aparece una sola vez (no .com.cl). */
    public static String correo(String email) {
        String v = email == null ? "" : email.trim().toLowerCase();
        Matcher m = CORREO.matcher(v);
        if (!m.matches() || m.group(1).startsWith(".") || m.group(1).endsWith(".") || m.group(1).contains("..")) {
            throw new SolicitudInvalidaException("Escribe un correo válido, por ejemplo nombre@dominio.cl");
        }
        String[] etiquetas = m.group(2).split("\\.");
        if (etiquetas.length < 2 || Arrays.stream(etiquetas).anyMatch(e -> !e.matches("^[a-z0-9-]+$"))) {
            throw new SolicitudInvalidaException("Escribe un correo válido, por ejemplo nombre@dominio.cl");
        }
        String fin = etiquetas[etiquetas.length - 1];
        if (!fin.equals("com") && !fin.equals("cl")) {
            throw new SolicitudInvalidaException("El correo debe terminar en .com o .cl");
        }
        for (int i = 0; i < etiquetas.length - 1; i++) {
            if (etiquetas[i].equals("com") || etiquetas[i].equals("cl")) {
                throw new SolicitudInvalidaException("Usa un solo .com o .cl: no se permite .com.cl ni terminaciones repetidas");
            }
        }
        return v;
    }

    /** Valida el RUT chileno con dígito verificador (módulo 11). Devuelve null si viene vacío. */
    public static String rut(String rut) {
        if (rut == null || rut.isBlank()) {
            return null;
        }
        String limpio = rut.replace(".", "").replace(" ", "").toUpperCase();
        Matcher m = RUT.matcher(limpio);
        if (!m.matches()) {
            throw new SolicitudInvalidaException("RUT inválido. Formato: 12.345.678-5");
        }
        String cuerpo = m.group(1);
        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int resto = 11 - (suma % 11);
        String dv = resto == 11 ? "0" : resto == 10 ? "K" : String.valueOf(resto);
        if (!dv.equals(m.group(2))) {
            throw new SolicitudInvalidaException("El dígito verificador del RUT no es correcto");
        }
        return limpio;
    }
}
